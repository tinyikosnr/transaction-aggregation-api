# Transaction Aggregation API

A modular-monolith Spring Boot service that ingests financial transactions from multiple upstream sources, validates and categorises them, and exposes secure REST APIs for retrieval and financial aggregation.

> **Status:** Foundational / early implementation. The SAD, accepted ADRs, and TDS in [`documentation/`](documentation/) form the current approved design baseline and are being implemented incrementally, one bounded context at a time. Documented conflicts between sources must be resolved before implementing the affected area. See [Development Workflow](#development-workflow) for the delivery order.

---

## Overview

The Transaction Aggregation API is a Java 21 / Spring Boot 4 backend designed to consolidate financial transactions from multiple independent providers into a single, standardised domain model — validating, deduplicating, categorising, and persisting transactions, and producing customer, category, merchant, and monthly financial summaries through a versioned REST API. This is the approved target design (see [Planned Capabilities](#planned-capabilities)); the codebase itself is still foundational.

The project is intentionally built as a **Modular Monolith** using **Domain-Driven Design** and **Clean Architecture** — a single deployable application with strict internal module boundaries, offering the operational simplicity of a monolith with a clear, deliberate path to microservice extraction if it's ever justified.

## Purpose

- Provide one consistent integration point for transaction data instead of many bespoke provider integrations.
- Apply consistent validation, deduplication, and categorisation rules across all sources.
- Produce reliable, auditable financial reporting (income, expenditure, net cash flow) per customer.
- Demonstrate a production-grade backend built to enterprise architecture standards, not just a working prototype.

## Business Problem

Organisations that receive transaction data from multiple independent systems typically face fragmented processing logic, inconsistent categorisation, and duplicated aggregation code across integrations — each provider has its own payload shape and conventions. This erodes reporting accuracy and increases the cost of every new integration or reporting requirement.

The Transaction Aggregation API centralises ingestion, validation, categorisation, persistence, and aggregation behind one platform and one API contract, so downstream consumers never need to know how many upstream sources exist or how they differ.

Full business context: [`Solution_Architecture_Document(SAD)_v_2_Part_1_Foundation.md`](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_1_Foundation.md).

## Architecture Summary

| Layer | Responsibility |
|---|---|
| Presentation (`api`) | REST controllers, request/response DTOs, validation, exception mapping |
| Application | Use cases / orchestration per module |
| Domain | Aggregates, entities, value objects, business invariants — framework-free |
| Infrastructure | JPA persistence adapters, security, external configuration |

The application is organised into cooperating business modules, each owning its own data and exposing only explicit interfaces (ports) to the rest of the system:

```
Shared → Transaction ─┬─→ Categorisation
                       ├─→ Audit
                       └─→ Merchant

Aggregation → Transaction, Customer   (read-only, via query ports)
```

- Modules never reach into another module's repository — cross-module access happens through application interfaces or domain events.
- The `Aggregation` module owns no tables of its own; it computes summaries from persisted transaction data.
- Module boundaries are intended to be verified with **Spring Modulith** (`spring-modulith-starter-test`), so illegal dependencies fail the build rather than surviving as review comments.

Full diagrams (C4 context/container, module dependencies, component, ER, sequence flows): [`Solution_Architecture_Document(SAD)_v_2_Part_3_Architecture.md`](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_3_Architecture.md) and [Part 4](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_4_Domain_and_Data.md).

## Technology Stack

| Concern | Technology |
|---|---|
| Language / runtime | Java 21 |
| Application framework | Spring Boot 4.1.0 |
| Modular monolith boundaries | Spring Modulith 2.1.0 (core, insight, JPA, runtime) |
| Web | Spring Web MVC |
| Persistence | Spring Data JPA (Hibernate) |
| Database | PostgreSQL |
| Migrations | Flyway (`flyway-database-postgresql`) |
| Security | Spring Security + OAuth2 Resource Server (JWT bearer) |
| Validation | Spring Validation (Jakarta Bean Validation) |
| Observability | Spring Boot Actuator, Micrometer, Prometheus registry |
| Build | Maven (via `mvnw` wrapper) |
| Containers (dev) | Docker Compose, Spring Boot Docker Compose support |
| Testing | JUnit 5, Mockito, Testcontainers (PostgreSQL), Spring Boot Test, Spring Modulith Test |

See [`Solution_Architecture_Document(SAD)_v_2_Part_6B_Governance_and_Reference.md`](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_6B_Governance_and_Reference.md) 52 for the full, versioned technology decision list and rationale (ADRs).

## Planned Capabilities

None of the capabilities below exist in source code yet — the repository currently contains only the generated Spring Boot application entry point and generated test scaffolding (see [Project Structure](#project-structure)). These capabilities are **approved in the SAD and TDS** and will be implemented incrementally, one bounded context at a time, following the order in [Development Workflow](#development-workflow). This list will be updated to reflect what's actually implemented as each feature branch lands.

- Single and bulk transaction ingestion, with per-item partial success on bulk requests.
- Strict request validation and business-invariant validation (amount, currency, timestamps, identifiers).
- Duplicate detection on `(transaction source, external transaction ID)`, enforced at both application and database level.
- Configurable, priority-ordered categorisation rules (merchant and description matching) with a fallback category.
- Merchant name normalisation for consistent reporting.
- Transaction search with filtering, pagination, and whitelisted sorting.
- Customer, category, merchant, and monthly financial summaries.
- Append-only audit trail for business-significant events, keyed by correlation ID.
- JWT-secured, role/authority-based REST API.
- RFC 9457 (`application/problem+json`) error responses with a stable application error-code catalogue.
- Health, readiness, and metrics endpoints via Spring Boot Actuator.

Full functional and non-functional requirements: [Part 2 – Requirements](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_2_Requirements.md).

## Project Structure

Current repository layout:

```
transaction-aggregation-api
├── documentation/                 Architecture and design documents (source of truth)
├── src/main/java/.../             Application entry point (za.co.tinyiko.transactionaggregation)
├── src/main/resources/            application.properties
├── src/test/java/.../             Test bootstrap and Testcontainers configuration
├── compose.yaml                   Local PostgreSQL for development/tests
├── pom.xml
├── mvnw / mvnw.cmd
└── HELP.md                        Spring Initializr reference notes
```

The full target package layout (module-per-package, with a shared top-level `api` layer) is defined in the Technical Design Specification and is the structure new code should follow: [Technical Design Specification, Part 1](documentation/Transaction_Aggregation_API_Technical_Design_Specification.md#part-1--project-structure-and-package-design).

## Module Overview

| Module | Owns | Responsibility |
|---|---|---|
| `shared` | — | Cross-cutting abstractions, exceptions, utilities. No business rules. |
| `transaction` | `transactions`, `transaction_sources` | Ingestion, validation, duplicate detection, persistence, search |
| `customer` | `customers` | Customer identity and lookup for other modules |
| `merchant` | `merchants` | Merchant name normalisation and resolution |
| `categorisation` | `transaction_categories`, `categorisation_rules` | Rule evaluation and category assignment |
| `aggregation` | *(none)* | Financial summaries, computed read-only from transaction data |
| `audit` | `audit_events` | Append-only business event trail |
| `security` / `config` | — | JWT validation, method security, cross-cutting configuration |

Ownership rules, dependency directions, and data-access rules: [Part 4 – Domain & Data, 31](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_4_Domain_and_Data.md) and [Part 3 – Architecture, 21](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_3_Architecture.md).

## Running Locally

**Prerequisites:** JDK 21, Docker Desktop (for PostgreSQL and Testcontainers). Maven is not required — use the bundled wrapper.

```bash
# 1. Clone the repository
git clone <repository-url>
cd transaction-aggregation-api

# 2. Run the application
./mvnw spring-boot:run          # Linux/macOS
mvnw.cmd spring-boot:run        # Windows
```

Because `spring-boot-docker-compose` is on the classpath, Spring Boot will automatically start and stop the PostgreSQL container defined in `compose.yaml` when the application starts and stops — a manual `docker compose up` is only needed if you want the database running independently of the app (e.g. to inspect it directly).

The application starts on the default port `8080` under the `transaction-aggregation-api` application name.

## Docker

`compose.yaml` defines a single local development service:

```yaml
postgres: postgres:latest, database "mydatabase", user "myuser"
```

These credentials are **local-development placeholders only** and must never be reused in a shared or production environment — see [Security](#security). A production application image (multi-stage Dockerfile) is planned per [Part 6A – Operations, 43](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_6A_Operations.md) but is not yet present in this repository.

```bash
docker compose up      # start PostgreSQL
docker compose down    # stop and remove the container
```

## Flyway

Flyway is on the classpath (`flyway-database-postgresql`) and will run automatically against the configured PostgreSQL instance on startup. The migration directory (`src/main/resources/db/migration`) already exists but is currently empty — no migration scripts have been added yet. The schema defined in the [Technical Design Specification, Part 5](documentation/Transaction_Aggregation_API_Technical_Design_Specification.md#part-5--database-design) is the authoritative starting point for the first migrations.

Naming convention, once migrations exist:

```
V1__create_core_tables.sql
V2__create_categorisation_tables.sql
V3__create_audit_table.sql
V4__seed_transaction_categories.sql
V5__seed_categorisation_rules.sql
```

Applied migrations are immutable; schema changes are always additive new migration files. See [Part 4 – Domain & Data, 29.5](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_4_Domain_and_Data.md) for full rules.

## Testing

```bash
./mvnw test       # unit + integration tests (starts Testcontainers PostgreSQL — Docker must be running)
./mvnw verify      # full build-verification lifecycle
```

The test stack uses JUnit 5, Mockito, Spring Boot Test, Spring Modulith's test starter (module boundary verification), and Testcontainers for real PostgreSQL integration tests — no mocked database in integration tests. Testcontainers-based tests provision their own PostgreSQL container via `TestcontainersConfiguration` and do not depend on, or interact with, the `compose.yaml` database described under [Docker](#docker) — the two are independent container lifecycles, and Docker must be running for either. The testing pyramid, required test types per layer, and the required test list for the first implementation slice are defined in [Part 6A – Operations, 45](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_6A_Operations.md) and [TDS 70](documentation/Transaction_Aggregation_API_Technical_Design_Specification.md#70-recommended-first-implementation-slice).

## API Documentation

The API is versioned under `/api/v1`. OpenAPI/Swagger generation (`springdoc-openapi`) is specified in the architecture but not yet added as a build dependency; once added, documentation will be served at `/v3/api-docs` and `/swagger-ui.html`, with Swagger UI access restricted or disabled in production. Until then, the authoritative API contracts (request/response payloads, status codes, filtering, pagination, sorting) are documented in [Part 5 – API & Security, 34–35](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_5_API_and_Security.md) and [TDS Part 6](documentation/Transaction_Aggregation_API_Technical_Design_Specification.md#part-6--api-contract).

All errors use RFC 9457 Problem Details (`application/problem+json`) with a stable `errorCode` and `correlationId` — see the error catalogue in [SAD 39.4](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_5_API_and_Security.md) and [TDS 40](documentation/Transaction_Aggregation_API_Technical_Design_Specification.md#40-error-codes).

## Security

- Bearer JWT authentication via `spring-boot-starter-security-oauth2-resource-server`.
- **Authorities protect individual operations** and are what `@PreAuthorize` evaluates (`TRANSACTION_READ`, `TRANSACTION_WRITE`, `CUSTOMER_READ`, `AGGREGATION_READ`, `CATEGORY_ADMIN`, `AUDIT_READ`, `OPERATIONS_READ` — SAD 36.4).
- **Roles are named collections of authorities** assigned to a client or user (`ROLE_API_CONSUMER`, `ROLE_SUPPORT`, `ROLE_ADMIN` — TDS 42). A JWT's `roles` claim is expanded into granted authorities at authentication time; role names are never checked directly. The approved role-to-authority mapping is documented in ADR-016 (SAD 49.1) — this was previously an open conflict between the SAD and TDS and is now resolved.
- HTTPS is mandatory outside local development; secrets (DB credentials, JWT signing/verification material) must come from environment variables or a managed secret store, never source control.
- Actuator endpoint exposure must be explicitly restricted before any shared deployment — liveness/readiness may be public, `metrics`/`env`/`loggers` must not be.

Full threat model, JWT claim validation rules, and header/CORS policy: [Part 5 – API & Security, 36](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_5_API_and_Security.md). Role/authority model: [Part 5, 36.4](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_5_API_and_Security.md), [TDS 42](documentation/Transaction_Aggregation_API_Technical_Design_Specification.md#42-roles-and-authority-mapping), and ADR-016 ([SAD 49.1](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_6B_Governance_and_Reference.md)).

## Documentation Folder

[`documentation/`](documentation/) is the source of truth for this project. Read it before making architectural changes.

| File | Contents |
|---|---|
| [`Solution_Architecture_Document(SAD)_v_1.md`](documentation/Solution_Architecture_Document%28SAD%29_v_1.md) | Original diagram-first SAD (superseded by v2, kept for history) |
| [`..._v_2_Part_1_Foundation.md`](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_1_Foundation.md) | Executive summary, business context, scope, principles, quality attributes |
| [`..._v_2_Part_2_Requirements.md`](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_2_Requirements.md) | Functional/non-functional requirements, use cases, business rules, traceability |
| [`..._v_2_Part_3_Architecture.md`](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_3_Architecture.md) | C4 diagrams, module design, ADRs |
| [`..._v_2_Part_4_Domain_and_Data.md`](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_4_Domain_and_Data.md) | Domain model, entities, value objects, database design, data ownership, integrity |
| [`..._v_2_Part_5_API_and_Security.md`](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_5_API_and_Security.md) | REST API design, contracts, security, logging, observability, error handling |
| [`..._v_2_Part_6A_Operations.md`](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_6A_Operations.md) | Deployment, Docker, configuration, testing strategy, scalability, microservice evolution |
| [`..._v_2_Part_6B_Governance_and_Reference.md`](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_6B_Governance_and_Reference.md) | ADR register, risks, roadmap, tech stack, glossary, references |
| [`Transaction_Aggregation_API_Technical_Design_Specification.md`](documentation/Transaction_Aggregation_API_Technical_Design_Specification.md) | Implementation blueprint: package structure, class catalogue, DDL, API contracts, error codes, event catalogue |

Documentation must be kept in sync with the code — see [`CLAUDE.md`](CLAUDE.md#documentation-rules).

## Future Roadmap

**Functional:** multi-currency support, user-defined categorisation rules, scheduled recurring reports, additional provider integrations, notifications/subscriptions.

**Technical:** distributed caching, event-driven integration, CQRS read models, materialised reporting views, search optimisation.

**Platform / evolution:** the modular monolith is designed for eventual extraction of the Transaction, Customer, Categorisation, Aggregation, and Audit modules into independently deployable services — only once justified by measurable scaling, ownership, or release-cadence needs. Kubernetes, a service mesh, and a centralised API gateway are explicitly out of scope for the current release.

Full list: [Part 6B – Governance & Reference, 51](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_6B_Governance_and_Reference.md) and [47 Evolution to Microservices](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_6A_Operations.md).

## Build Commands

```bash
./mvnw clean              # remove build output
./mvnw compile             # compile sources
./mvnw test                 # run tests (Docker required — Testcontainers)
./mvnw verify              # full verification lifecycle
./mvnw package              # build the executable jar
./mvnw spring-boot:run     # run the application
```

On Windows, substitute `mvnw.cmd` for `./mvnw`.

## Development Workflow

Implementation proceeds **one bounded context at a time**, each independently compilable before the next begins:

```
feature/project-structure → feature/shared → feature/customer → feature/merchant →
feature/categorisation → feature/audit → feature/transaction → feature/aggregation
```

`feature/project-structure` comes first and only establishes the package skeleton, Spring Modulith module boundaries, architecture verification tests, and shared configuration structure — no business logic. `categorisation` and `audit` are built before `transaction` because the transaction ingestion workflow depends on both (assigning a category and recording an audit event are part of processing a transaction, not features bolted on afterwards). `aggregation` stays last because it only reads transaction data that must already exist. See [`CLAUDE.md`](CLAUDE.md#implementation-rules) for the full rationale. Before implementing a feature:

1. Read the relevant sections of `documentation/` for that module.
2. Confirm the module's package structure, ports, and dependency rules.
3. Implement with tests (unit, repository/integration, controller, and module-boundary tests as applicable).
4. Update `documentation/` if the implementation changes an architectural decision.

See [`CLAUDE.md`](CLAUDE.md) for the full engineering standards and workflow AI-assisted contributions should follow — the same standards apply to human contributions.

## Contribution Guidelines

- Branch per bounded context/feature (`feature/<module>` or `fix/<short-description>`).
- Run `./mvnw verify` locally before opening a pull request.
- New behaviour requires tests; keep coverage aligned with the testing strategy in [SAD 45](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_6A_Operations.md).
- Respect module boundaries and dependency rules — do not add controller-to-repository or cross-module repository access.
- Never expose JPA entities through the REST API; always map to/from DTOs.
- If a change affects an architectural decision, update the SAD/TDS/ADRs in the same pull request (see [Documentation Rules](CLAUDE.md#documentation-rules)).
- Keep pull requests scoped to a single module or concern where practical.

## License

No license has been declared for this project yet. Add a `LICENSE` file before public release or external distribution.
