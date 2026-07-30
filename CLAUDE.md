# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## Project Overview

The **Transaction Aggregation API** is a Spring Boot 4.1.0 REST API (Java 21, Maven) that consolidates financial transactions from multiple upstream sources into a single, standardised domain model. It ingests transactions (single and bulk), validates and deduplicates them, assigns categories through configurable rules, persists them in PostgreSQL, and exposes secure REST APIs for retrieval and financial aggregation (customer, category, merchant, and monthly summaries).

**Business purpose:** replace fragmented, per-provider transaction processing with one centralised platform, so every downstream consumer integrates with a single consistent API instead of duplicating validation, categorisation, and aggregation logic per source.

**Architecture:** a **Modular Monolith** built on **Domain-Driven Design** and **Clean Architecture**. It is one deployable Spring Boot application with strict internal module boundaries — each module owns its own data and exposes only explicit interfaces to the rest of the system. This gives strong transactional consistency and simple deployment today, with a deliberate, documented path to extracting modules into independent services later if scaling or ownership needs justify it. Do not introduce microservice-style infrastructure (service discovery, message brokers, API gateways) speculatively — that evolution happens only when the readiness indicators in the SAD are met.

**Goals:** correctness of financial data above all else (immutable transactions, no duplicate processing, exact decimal money), followed by maintainability (clean module boundaries that are actually enforced, not just documented), then observability and security as first-class, non-optional concerns.

This repository is in an early implementation phase. The SAD, accepted ADRs, and TDS form the current approved design baseline in `documentation/` — treat it as the source of truth, not as inspiration to be reinterpreted. Documented conflicts must be resolved (with an ADR, if the resolution is itself an architectural decision) before implementing the affected area. See [Documentation Precedence](#documentation-precedence) for how to resolve a disagreement between sources.

## Technology Stack

| Technology | Version | Notes |
|---|---|---|
| Java | 21 | Language baseline (`java.version` in `pom.xml`) |
| Spring Boot | 4.1.0 | Parent POM |
| Spring Modulith | 2.1.0 | `core`, `insight`, `jpa`, `runtime` starters — module boundary enforcement |
| Spring Web MVC | (Boot-managed) | REST layer |
| Spring Data JPA / Hibernate | (Boot-managed) | Persistence |
| PostgreSQL | `postgres:latest` locally | Pin an explicit version before any shared/prod environment |
| Flyway | `flyway-database-postgresql` | Schema migrations; no migrations exist yet in this repo |
| Spring Security + OAuth2 Resource Server | (Boot-managed) | JWT bearer authentication |
| Spring Validation | (Boot-managed) | Jakarta Bean Validation |
| Spring Boot Actuator + Micrometer (Prometheus) | (Boot-managed) | Health, metrics |
| Maven | wrapped (`mvnw`, Maven 3.9.16) | Do not require a system-installed Maven |
| Docker / Docker Compose | — | Local PostgreSQL via `compose.yaml`; auto-managed by `spring-boot-docker-compose` |
| JUnit 5, Mockito, Testcontainers (PostgreSQL), Spring Boot Test, Spring Modulith Test | (Boot/Modulith-managed) | Test stack |

**Not yet dependencies — do not assume they exist without adding them first:**
- `springdoc-openapi` (OpenAPI/Swagger UI is specified in the SAD but not yet in `pom.xml`).
- Lombok (not currently used; if introduced, see [Coding Rules](#coding-rules) — never on JPA entities).
- ArchUnit as a project dependency — not declared directly, though `com.tngtech.archunit:archunit` is present transitively (runtime scope, pulled in by `spring-modulith-runtime`, which uses it internally). Do not write test code against that transitive copy — it's an implementation detail of Modulith, not a stable API for this project to depend on. Module-boundary enforcement currently relies entirely on Spring Modulith's own `ApplicationModules.of(...).verify()`, which covers cross-module boundaries but not intra-module layering. The one documented rule it can't express — that `domain` packages must not depend on Spring, Jakarta Persistence, or presentation types — is deliberately unenforced for now: no `domain` package contains a class yet, so there is nothing to check. Revisit (with an explicitly declared `archunit-junit5` test dependency, not the transitive one) once the first domain model is implemented, rather than adding it against an empty package.

## Commands

On Windows use `mvnw.cmd`; on Unix use `./mvnw`.

```bash
./mvnw compile             # compile sources
./mvnw test                 # run tests (Docker must be running — Testcontainers)
./mvnw verify               # full verification lifecycle (tests + quality gates)
./mvnw package               # build the executable jar
./mvnw spring-boot:run      # run the application locally
./mvnw clean                # remove build output

docker compose up            # start local PostgreSQL (compose.yaml) — optional, see below
docker compose down           # stop and remove it
```

`spring-boot-docker-compose` is on the classpath as an optional runtime dependency, so running the application (`./mvnw spring-boot:run`) starts and stops `compose.yaml`'s PostgreSQL container automatically. This is a separate lifecycle from testing: `./mvnw test` does not use `compose.yaml` at all — `TestcontainersConfiguration` (`src/test/java/.../TestcontainersConfiguration.java`) provisions its own PostgreSQL container via Testcontainers for the test run, independent of whatever `compose.yaml` is doing. Docker must be running for both scenarios, but they are two distinct containers. Run `docker compose up` manually only when PostgreSQL needs to stay available independently of the running application (e.g. to inspect data with `psql`) — it is not required for `./mvnw test`.

## Architecture

### Style

Modular Monolith + Domain-Driven Design + Clean Architecture, layered per module:

```
Presentation (api)  →  Application (use cases)  →  Domain (entities, value objects, invariants)  →  Infrastructure (JPA adapters, security, config)
```

The **domain layer must never depend on Spring, Jakarta EE, or any persistence/framework type.** It expresses business rules in plain Java (records for value objects, plain classes for entities). Framework annotations belong in the infrastructure and presentation layers only.

### Package Responsibilities

Base package: `za.co.tinyiko.transactionaggregation`. The 10 top-level module packages listed below already exist (`feature/project-structure`) as an empty skeleton: each has a `package-info.java` documenting its responsibility, and `shared` carries an explicit Spring Modulith `@ApplicationModule` declaration (see [Module Boundaries and Dependency Rules](#module-boundaries-and-dependency-rules)). The internal per-module layering shown below (`domain`, `application`, `port`, etc.) is the target structure from the Technical Design Specification and does not exist as physical packages yet — it is created incrementally, only as each module's feature branch adds real code to it.

```
za.co.tinyiko.transactionaggregation
├── api                     # shared presentation layer — controllers, request/response DTOs, exception advice
│   ├── controller
│   ├── request
│   ├── response
│   └── advice
├── transaction             # owns transactions, transaction_sources
│   ├── domain
│   ├── application         # use-case interfaces + implementations
│   ├── port                 # outbound interfaces (repository ports, query ports)
│   ├── persistence           # JPA entities, Spring Data repositories, port adapters
│   ├── mapper
│   ├── event
│   └── validation
├── categorisation          # owns transaction_categories, categorisation_rules
│   ├── domain / application / port / persistence / rule / event
├── aggregation              # owns no tables — reads via transaction's query port
│   ├── domain / application / port / persistence / mapper
├── customer                 # owns customers
│   ├── domain / application / port / persistence
├── merchant                  # owns merchants
│   ├── domain / application / port / persistence
├── audit                     # owns audit_events (append-only)
│   ├── domain / application / port / persistence
├── security                  # JWT validation, role/authority extraction, access-denied handling
├── config                     # cross-cutting Spring configuration (Jackson, JPA, clock, correlation ID filter, OpenAPI)
└── shared                     # reusable technical building blocks only — exceptions, utilities, event envelope
```

Controllers live in the shared top-level `api` package (thin, no business logic) and call into each module's `application` use-case interfaces — the `api` layer must never depend on a module's `domain`, `port`, or `persistence` types directly, only its `application`-layer interfaces.

### Module Boundaries and Dependency Rules

- `shared` contains only reusable, business-rule-free abstractions (exception hierarchy, event envelope, utilities). Every module may depend on it; it depends on nothing else in the codebase.
- Allowed module dependency directions: `transaction → categorisation`, `transaction → audit`, `transaction → merchant`, `aggregation → transaction` (read-only), `aggregation → customer` (read-only). Do not introduce a dependency running the other way, and never a cycle.
- A module may **never** access another module's JPA repository or persistence package directly. Cross-module reads/writes go through the target module's `application` use-case interfaces or `port` query interfaces; asynchronous reactions go through domain events.
- Controllers never depend on repositories, only on `application` use-case interfaces.
- Persistence entities are never returned from a controller or crossed a module boundary — map to/from DTOs and domain types at the edges.
- The `aggregation` module owns no persistent tables and must not write transaction data; it composes summaries by reading through `transaction`'s query port.
- These rules should be enforced as executable tests using Spring Modulith (`ApplicationModules.of(TransactionAggregationApiApplication.class).verify()`, in `ModularityTests` under the `architecture` test package), not left as documentation-only conventions. Treat a failing module-verification test the same as a failing compile.
- Modules are recognised by Spring Modulith's default package-based convention alone — a bare `package-info.java` is enough, no annotation required. `verify()`'s built-in encapsulation of nested (non-root) packages already enforces "no direct cross-module persistence access" and "controllers cannot depend on repositories" with zero extra configuration. Add an explicit `@ApplicationModule` annotation only where a rule genuinely cannot be expressed any other way: currently only `shared` carries one (`type = OPEN, allowedDependencies = {}`), because "shared may not depend on business modules" has no other enforcement mechanism. Do not add annotations to other modules "for documentation" — the package-info Javadoc already documents intent, and an annotation should only appear when it changes verified behaviour.

### Aggregate Ownership

| Aggregate root | Module | Notes |
|---|---|---|
| `Customer` | customer | Owns customer identity/status lifecycle |
| `Transaction` | transaction | Immutable once successfully processed; sole writer of `transactions` |
| `TransactionSource` | transaction | Reference data for ingestion sources |
| `Merchant` | merchant | Normalised merchant identity |
| `TransactionCategory`, `CategorisationRule` | categorisation | Two aggregate roots in one module |
| `AuditEvent` | audit | Append-only — no update or delete operations, ever |

Each module is the only writer of its own tables (see the ownership matrix in SAD §31.2). Other modules read owned data only through the owning module's ports, never through direct table/repository access.

### Ports and Adapters

Within each module, `port` defines outbound interfaces the domain/application layer needs (e.g. `TransactionRepositoryPort`, `CustomerLookupPort`, `DomainEventPublisher`). `persistence` provides the adapters that implement those ports using Spring Data JPA (e.g. a `JpaTransactionRepositoryAdapter` wrapping a `SpringDataTransactionRepository`). Inbound ports are the `application` use-case interfaces that controllers call. New integrations (a future message broker, an external HTTP client) get their own adapter implementing an existing or new port — the domain/application layer never depends on the adapter's concrete type.

### Transaction Boundaries

- One HTTP write request executes within one application-level transaction, owned by the `application`-layer use-case, not the controller and not the repository.
- Bulk ingestion processes each item in its own transaction boundary so a single invalid item never rolls back the rest of the batch (per-item partial success is a hard requirement — see SAD BR rules and the bulk API contract).
- Do not place long-running or external (network) calls inside a database transaction.
- Optimistic locking (`@Version`) protects mutable aggregates from lost updates; immutable processed transactions don't need update-oriented locking after creation.

### Adding a New Module

1. Create a new top-level package under the base package with `domain`, `application`, `port`, `persistence` (and `mapper`/`event`/`rule` as needed).
2. Define the domain model first (entities, value objects, invariants) with no framework dependencies.
3. Define outbound ports as interfaces before writing the JPA adapter that implements them.
4. Add the module to the dependency diagram and ownership matrix in the SAD, and get the dependency direction agreed before writing code — this is not a mechanical step, it changes the architecture.
5. Add a Flyway migration for any new tables, following the `V{n}__description.sql` convention.
6. Add or extend `ModularityTests` (under the `architecture` test package) to cover the new module's allowed dependencies.
7. Follow the [Implementation Rules](#implementation-rules) — implement it as its own `feature/<module>` slice, independently compilable.

### Where New Things Belong

| Adding a... | Goes in |
|---|---|
| REST endpoint / controller | `api.controller` (shared top-level), thin, delegates to the module's `application` use case |
| Request/response DTO | `api.request` / `api.response` |
| JPA / domain entity | `<module>.domain` (plain domain model) + a corresponding JPA entity in `<module>.persistence` — these are two distinct types, never the same class |
| Repository | Interface in `<module>.port`; Spring Data interface + adapter implementation in `<module>.persistence` |
| DTO-level validation | Jakarta Bean Validation annotations on `api.request` types |
| Business-invariant validation | Inside `<module>.domain` (constructors/factory methods enforcing invariants) and `<module>.application` (use-case-level checks) — validation is layered, not just done once at the API boundary |
| Mapper | `<module>.mapper` (e.g. an API mapper between `api` DTOs and `application` types, and a persistence mapper between domain and JPA entities) |
| Domain event | `<module>.event`, published through the `DomainEventPublisher` port |

## Development Principles

Claude must always behave as a **Senior Software Engineer, Software Architect, Backend Engineer, Code Reviewer, Security Engineer, and Performance Engineer** simultaneously — not just whichever hat is most convenient for the immediate request.

Before writing any code:

1. Understand the requirement completely — re-read the relevant `documentation/` sections rather than guessing.
2. Explain the proposed solution in plain terms.
3. Identify risks, trade-offs, and anything ambiguous in the requirement.
4. Produce an implementation plan (files touched, module(s) affected, tests to add).
5. For a large or architecturally significant feature, wait for explicit approval of the plan before writing code. "Large" means: touches more than one module, changes a public port/interface, changes the database schema, or changes a security rule.

Never jump directly into coding. A fast wrong answer is worse than a slightly slower correct one on a financial system.

## Engineering Standards

Enforce, in order of how often they get sacrificed for expediency:

- SOLID
- Domain-Driven Design
- Clean Architecture
- Clean Code
- DRY — but not at the cost of coupling unrelated modules together
- KISS
- YAGNI — do not build for a future requirement that isn't documented in the SAD/TDS
- Composition over inheritance
- Dependency inversion (depend on ports/interfaces, not concrete infrastructure types)
- Immutability where appropriate (value objects, DTOs, processed transactions)
- Meaningful naming — no abbreviations that aren't already established in the domain glossary (SAD §53)
- Small methods, small classes, single responsibility
- Constructor injection only — **no field injection, ever**
- Prefer interfaces at module boundaries; concrete classes are fine as private implementation detail
- Avoid premature optimisation, but do not ignore an obvious N+1 query or unbounded query

## Coding Rules

- Java 21 and Spring Boot 4 idioms throughout.
- Use records for value objects and immutable DTOs where they fit; use plain classes for JPA entities and mutable aggregates.
- Lombok, if introduced, is used only where it removes genuine boilerplate (e.g. builders on request DTOs) — **never `@Data` on a JPA entity** (it generates `equals`/`hashCode`/`toString` that break lazy loading, identity semantics, and can leak associations into logs).
- Use `UUID` for all entity identifiers.
- Use `Instant` for all timestamps; persist as `TIMESTAMPTZ`; treat everything internally as UTC.
- Use `BigDecimal` for all monetary amounts — floating-point types are prohibited for money, no exceptions.
- Use optimistic locking (`@Version`) on mutable aggregates.
- Errors follow RFC 9457 — use Spring's `ProblemDetail`, never a bespoke error envelope.
- Validate all input: Bean Validation at the API boundary, explicit invariant checks in the domain/application layers. Never trust that API validation alone is sufficient.
- Never expose JPA entities through REST. Always map to/from purpose-specific DTOs.
- Whitelist any dynamically-provided sort field or query parameter before it reaches persistence — never pass client-supplied property names straight into a query.
- `@PreAuthorize` checks fine-grained authorities (`TRANSACTION_WRITE`, `CUSTOMER_READ`, etc.), never role names. Roles (`ROLE_API_CONSUMER`, `ROLE_SUPPORT`, `ROLE_ADMIN`) are collections of authorities assigned to a client; the approved mapping is ADR-016 (SAD §49.1, TDS §42). This is documentation only — the Spring Security configuration itself is not yet implemented.

## Testing Rules

Follow the testing strategy and the required-tests list in the Technical Design Specification (§70) and SAD §45.

Prefer, in this order of value for this codebase:

1. **Unit tests** for domain and application logic (JUnit 5 + Mockito) — no Spring context.
2. **Repository tests** against a real PostgreSQL via Testcontainers — never mock the database for persistence-layer tests.
3. **Integration tests** that exercise a use case end-to-end within the Spring context.
4. **Controller tests** for request validation, status codes, and error-response shape.
5. **Architecture tests** — Spring Modulith's `ApplicationModules.verify()`, in `ModularityTests` under the `architecture` test package — to make module boundary violations a build failure, not a review comment.

Every new feature needs, at minimum: the happy path, each documented validation failure, and each documented error-code scenario for that endpoint/use case.

## Documentation Rules

Whenever a major architectural decision changes (module boundaries, aggregate ownership, API contract, security model, database schema), update, in the same change:

- `README.md`
- The relevant SAD part(s) in `documentation/`
- The TDS
- The ADR register (SAD §49) — add a new `ADR-0XX` entry; do not silently edit the rationale of an existing accepted ADR, mark it superseded instead

Documentation must never be allowed to drift behind the code. If a change to `documentation/` is out of scope for the current task, say so explicitly rather than skipping it silently.

## Documentation Precedence

When two documentation sources disagree, resolve the disagreement using this precedence order — highest first:

1. Accepted ADRs (the register in SAD §49)
2. The current approved SAD files — those whose filenames contain `v_2` (`Solution_Architecture_Document(SAD)_v_2_Part_*.md`). The `v_2` filename is what makes a part authoritative; `Solution_Architecture_Document(SAD)_v_1.md` is superseded and retained only for history.
3. The Technical Design Specification (TDS)
4. `README.md`
5. `CLAUDE.md`

Filenames are authoritative for document versions. Do not rename a SAD file or change its version identifier just because its historical wording reads differently from a later decision — supersede outdated content with a new accepted ADR (or a new versioned SAD part) instead of editing history in place.

When sources conflict:

- Report the conflict rather than silently choosing one source.
- Identify the affected files and sections.
- Propose a resolution.
- Do not implement against the unresolved area until the conflict is recorded as resolved — normally via a new accepted ADR, per [Documentation Rules](#documentation-rules).

## Implementation Rules

Implement **one bounded context at a time**, in dependency order, each independently compilable and testable before moving to the next:

```
feature/project-structure → feature/shared → feature/customer → feature/merchant →
feature/categorisation → feature/audit → feature/transaction → feature/aggregation
```

**`feature/project-structure` comes first and establishes only:**

- the package skeleton for every module (`domain`, `application`, `port`, `persistence`, etc., as listed under [Package Responsibilities](#package-responsibilities))
- Spring Modulith module boundaries (the package layout that lets `ApplicationModules.of(...)` discover each module)
- the architecture verification test(s) that enforce those boundaries
- shared cross-cutting configuration structure (e.g. where `config` and `security` will live)

It must not implement any business logic, entity, controller, repository, or service — it only creates the skeleton the later feature branches build inside.

After that, `shared`, `customer`, and `merchant` come next because nothing else in the domain depends on anything they don't already have. `categorisation` and `audit` are established **before** `transaction` — not after — because the transaction ingestion workflow itself depends on both: a transaction cannot be persisted as `PROCESSED` without a categorisation decision, and every ingestion outcome (success, duplicate, rejection) must be recorded as an audit event as part of that same use case. Building `transaction` first would mean building it against capabilities that don't exist yet. `aggregation` remains last because it only reads persisted transaction data through a read-only query port — there is nothing for it to summarise until `transaction` exists.

Do not start a module whose dependencies aren't yet in place, and do not let a feature branch grow to span multiple modules — that's a signal the module boundary needs re-examining, not a reason to skip the branch split.
