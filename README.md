# Transaction Aggregation API

A modular-monolith Spring Boot service that ingests financial transactions from multiple upstream sources, validates and categorises them, and exposes REST APIs for retrieval and financial aggregation.

> **Status:** All planned bounded contexts are implemented (`project-structure` → `shared` → `customer` → `merchant` → `categorisation` → `audit` → `transaction` → `aggregation` → `api`). The REST API surface (`feature/api`) is wired up and tested end-to-end. **Security is a temporary permit-all placeholder** — real JWT authentication and authority-based authorization (`feature/security`) have not been implemented yet; see [Security](#security). The SAD, accepted ADRs, and TDS in [`documentation/`](documentation/) remain the source of truth. See [Development Workflow](#development-workflow) for the delivery order and current branch status.

---

## Overview

The Transaction Aggregation API is a Java 21 / Spring Boot 4 backend that consolidates financial transactions from multiple independent providers into a single, standardised domain model — validating, deduplicating, categorising, and persisting transactions, and producing customer, category, merchant, and monthly financial summaries through a REST API.

The project is built as a **Modular Monolith** using **Domain-Driven Design** and **Clean Architecture** — a single deployable application with strict internal module boundaries (enforced at build time by Spring Modulith), offering the operational simplicity of a monolith with a clear, deliberate path to microservice extraction if it's ever justified.

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
| Presentation (`api`) | REST controllers, request/response DTOs, Bean Validation, global exception mapping to RFC 9457 |
| Application | Use cases / orchestration per module |
| Domain | Aggregates, entities, value objects, business invariants — framework-free |
| Infrastructure | JPA persistence adapters, correlation-ID filter, security, configuration |

The application is organised into cooperating business modules, each owning its own data and exposing only explicit interfaces (ports) to the rest of the system:

```
Shared → Transaction ─┬─→ Categorisation
                       ├─→ Audit
                       └─→ Merchant

Aggregation → Transaction, Customer   (read-only, via query ports)

Api → Transaction, Aggregation, Categorisation, Shared   (presentation layer, calls application ports only)
```

- Modules never reach into another module's repository — cross-module access happens through `application`-layer ports/use-case interfaces, exposed cross-module via a package-level `@NamedInterface` where a real external caller exists.
- The `aggregation` module owns no tables of its own; it computes summaries from persisted transaction data through `transaction`'s read-only query port.
- Module boundaries are verified with **Spring Modulith** (`ApplicationModules.of(...).verify()` in `ModularityTests`) as part of the normal test suite — illegal dependencies fail the build, not just a review comment. A small hand-rolled reflection helper (`FrameworkIndependenceAssertions`) additionally enforces intra-module layering (domain stays framework-free; application never reaches into persistence).

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
| Security | Spring Security + OAuth2 Resource Server dependency present; **not yet configured** — see [Security](#security) |
| Validation | Spring Validation (Jakarta Bean Validation) |
| Observability | Spring Boot Actuator, Micrometer, Prometheus registry |
| Build | Maven (via `mvnw` wrapper) |
| Containers (dev) | Docker Compose, Spring Boot Docker Compose support |
| Testing | JUnit 5, Mockito, Testcontainers (PostgreSQL), Spring Boot Test (incl. `@WebMvcTest`), Spring Modulith Test |

See [`Solution_Architecture_Document(SAD)_v_2_Part_6B_Governance_and_Reference.md`](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_6B_Governance_and_Reference.md) 52 for the full, versioned technology decision list and rationale (ADRs).

## Implemented Capabilities

- Single transaction ingestion (`POST /api/v1/transactions`): validation, duplicate detection (application pre-check + database unique-constraint fallback), merchant resolution, rule-based categorisation, persistence, and an audit event — all within one transactional use case.
- Configurable, priority-ordered categorisation rules (merchant and description matching) with a seeded fallback category.
- Merchant name normalisation and find-or-create resolution.
- Customer, category, merchant, and monthly financial summaries (`GET /api/v1/customers/{customerId}/{summary|categories|merchants|monthly-summary}`), computed read-only from persisted transaction data over a caller-supplied date range.
- Append-only audit trail for business-significant events (transaction created, validation failed, duplicate rejected, source/customer not found), keyed by correlation ID.
- Correlation-ID propagation: honours a client-supplied `X-Correlation-ID`, generates one when absent, returns it on every response (success or error), threads it through to audit events, and places it in the logging MDC for the duration of the request.
- RFC 9457 (`application/problem+json`) error responses with an `errorCode`/`correlationId`/`timestamp` extension shape, covering every documented failure scenario for the implemented endpoints.

**Not yet implemented** (documented in the SAD/TDS but out of scope for the current branch — see [`CLAUDE.md`](CLAUDE.md) for the exact exclusion rationale):

- Bulk transaction ingestion, transaction retrieval/search (`GET /api/v1/transactions/**`).
- Any customer/merchant/categorisation-admin/audit CRUD or read endpoint (no documented HTTP contract, or no backing use case, for any of these today).
- Real authentication/authorization — see [Security](#security).
- OpenAPI/Swagger generation.

Full functional and non-functional requirements: [Part 2 – Requirements](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_2_Requirements.md).

## Project Structure

Current repository layout:

```
transaction-aggregation-api
├── documentation/                 Architecture and design documents (source of truth)
├── src/main/java/.../
│   ├── api/                       controller, dto (request/response), mapper, advice
│   ├── transaction/                domain, application, port, persistence, mapper (fully implemented)
│   ├── categorisation/              domain, application, port, persistence, mapper (fully implemented)
│   ├── aggregation/                 domain, application (fully implemented; owns no tables)
│   ├── customer/                    domain, application, port, persistence, mapper (fully implemented)
│   ├── merchant/                    domain, application, port, persistence, mapper (fully implemented)
│   ├── audit/                       domain, application, port, persistence, mapper (fully implemented)
│   ├── security/                    package skeleton only — no configuration yet
│   ├── config/                      ClockConfig, CorrelationIdFilter, SecurityConfig (temporary placeholder)
│   └── shared/                      event (DomainEventEnvelope), logging (CorrelationId)
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/               V1–V10 (customers, merchants, categorisation, audit, transactions + seeds)
├── src/test/java/.../             Unit, repository (Testcontainers), @WebMvcTest slice, one full
│                                    end-to-end smoke test, and architecture verification tests
├── compose.yaml                   Local PostgreSQL for development/tests
├── pom.xml
├── mvnw / mvnw.cmd
└── HELP.md                        Spring Initializr reference notes
```

Each module's internal layering (`domain`, `application`, `port`, `persistence`, etc.) is documented in [`CLAUDE.md`](CLAUDE.md#package-responsibilities). The full target package layout is defined in the Technical Design Specification: [Technical Design Specification, Part 1](documentation/Transaction_Aggregation_API_Technical_Design_Specification.md#part-1--project-structure-and-package-design).

## Module Overview

| Module | Owns | Responsibility |
|---|---|---|
| `shared` | — | Cross-cutting, business-rule-free abstractions: `DomainEventEnvelope`, `CorrelationId`. |
| `transaction` | `transactions`, `transaction_sources` | Ingestion, validation, duplicate detection, persistence; owns the create-transaction orchestration. |
| `customer` | `customers` | Customer identity, lookup and existence checks for other modules. |
| `merchant` | `merchants` | Merchant name normalisation and find-or-create resolution. |
| `categorisation` | `transaction_categories`, `categorisation_rules` | Rule evaluation and category assignment; category lookup by id. |
| `aggregation` | *(none)* | Financial summaries, computed read-only from transaction data via a query port. |
| `audit` | `audit_events` | Append-only business event trail. |
| `api` | *(none)* | REST controllers, request/response DTOs, Bean Validation, RFC 9457 error mapping. Depends only on each module's `application`-layer ports. |
| `security` / `config` | — | `config` holds cross-cutting configuration (`ClockConfig`, `CorrelationIdFilter`, temporary `SecurityConfig`); `security` is still an empty skeleton pending `feature/security`. |

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

The application starts on the default port `8080` under the `transaction-aggregation-api` application name. **Every endpoint is currently unauthenticated** (see [Security](#security)) — do not point this at a shared or public environment as-is.

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

Flyway (`flyway-database-postgresql`) runs automatically against the configured PostgreSQL instance on startup.

| Migration | Contents |
|---|---|
| `V1` | `customers` table |
| `V2` | `merchants` table |
| `V3` | `transaction_categories` table (partial unique index enforcing at most one fallback row) |
| `V4` | `categorisation_rules` table |
| `V5` | Seed `transaction_categories` |
| `V6` | Seed `categorisation_rules` |
| `V7` | `audit_events` table (append-only) |
| `V8` | `transaction_sources` table |
| `V9` | `transactions` table |
| `V10` | Seed `transaction_sources` |

Applied migrations are immutable; schema changes are always additive new migration files. See [Part 4 – Domain & Data, 29.5](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_4_Domain_and_Data.md) for full rules.

## Testing

```bash
./mvnw test       # unit + integration tests (starts Testcontainers PostgreSQL — Docker must be running)
./mvnw verify      # full build-verification lifecycle
```

The test stack uses JUnit 5, Mockito, Spring Boot Test (including `@WebMvcTest` slices for controllers, mocking the use-case layer — no database needed there), Spring Modulith's test starter (module boundary verification), and Testcontainers for real PostgreSQL integration tests — no mocked database in repository-layer tests. One `@SpringBootTest` smoke test (`CreateTransactionEndToEndTests`) exercises the full real stack (real Postgres, real filter chain, real security chain, real controller, real persistence) for the create-transaction happy path, to catch wiring mistakes a mocked-use-case slice test cannot. Testcontainers-based tests provision their own PostgreSQL container via `TestcontainersConfiguration` and do not depend on, or interact with, the `compose.yaml` database described under [Docker](#docker) — the two are independent container lifecycles, and Docker must be running for either. The testing pyramid, required test types per layer, and the required test list are defined in [Part 6A – Operations, 45](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_6A_Operations.md) and [TDS 70](documentation/Transaction_Aggregation_API_Technical_Design_Specification.md#70-recommended-first-implementation-slice).

## API Documentation

The API is versioned under `/api/v1`. OpenAPI/Swagger generation (`springdoc-openapi`) is specified in the architecture but deliberately deferred, consistent with `feature/security` (see [Part 6B, ADR register](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_6B_Governance_and_Reference.md)).

| Endpoint | Method | Description |
|---|---|---|
| `/api/v1/transactions` | `POST` | Create a single transaction. `201` + the created resource (`Location` header), `409` on duplicate, `404` on unknown source/customer, `400` on validation failure. |
| `/api/v1/customers/{customerId}/summary` | `GET` | Customer income/expenditure/net-cash-flow summary over `?from=&to=` (ISO dates). |
| `/api/v1/customers/{customerId}/categories` | `GET` | Debit totals grouped by category over the same date range. |
| `/api/v1/customers/{customerId}/merchants` | `GET` | Debit totals grouped by merchant over the same date range. |
| `/api/v1/customers/{customerId}/monthly-summary` | `GET` | Income/expenditure/net-cash-flow grouped by month over the same date range. |

The create-transaction response follows SAD 35.1's shape (including nested `merchant`/`category` objects), which is the authoritative source over TDS 28's narrower documented shape — see [`CLAUDE.md`](CLAUDE.md#documentation-precedence) for how documentation conflicts are resolved. The full, authoritative API contracts (request/response payloads, status codes, filtering, pagination, sorting for the not-yet-implemented endpoints) are documented in [Part 5 – API & Security, 34–35](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_5_API_and_Security.md) and [TDS Part 6](documentation/Transaction_Aggregation_API_Technical_Design_Specification.md#part-6--api-contract).

All errors use RFC 9457 Problem Details (`application/problem+json`) with a stable `errorCode`, `correlationId`, and `timestamp` — see the error catalogue in [SAD 39.4](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_5_API_and_Security.md) and [TDS 40](documentation/Transaction_Aggregation_API_Technical_Design_Specification.md#40-error-codes). One documented gap: `TransactionValidationException` currently maps to a generic `VALIDATION_ERROR` code rather than TDS 40's specific `TRX-002`/`TRX-004`/`TRX-005` codes, since the exception doesn't yet carry which invariant failed — giving it a structured reason is deferred work.

## Security

**Every endpoint is currently unauthenticated.** `spring-boot-starter-security` and the OAuth2 resource-server starter are on the classpath (added in `feature/project-structure` for the target JWT model) but not yet configured. `config.SecurityConfig` installs a **temporary, explicitly-flagged placeholder** `SecurityFilterChain` that permits every request and disables CSRF (appropriate for a stateless bearer-token REST API with no cookie-based session) — without it, Spring Boot's own default security auto-configuration would instead require HTTP Basic auth with a random per-boot password, which is not the target design either.

**Do not deploy this application outside local development as-is.** The target model:

- Bearer JWT authentication via `spring-boot-starter-security-oauth2-resource-server`.
- **Authorities protect individual operations** and are what `@PreAuthorize` evaluates (`TRANSACTION_READ`, `TRANSACTION_WRITE`, `CUSTOMER_READ`, `AGGREGATION_READ`, `CATEGORY_ADMIN`, `AUDIT_READ`, `OPERATIONS_READ` — SAD 36.4).
- **Roles are named collections of authorities** assigned to a client or user (`ROLE_API_CONSUMER`, `ROLE_SUPPORT`, `ROLE_ADMIN` — TDS 42). A JWT's `roles` claim is expanded into granted authorities at authentication time; role names are never checked directly. The approved role-to-authority mapping is documented in ADR-016 (SAD 49.1).
- HTTPS is mandatory outside local development; secrets (DB credentials, JWT signing/verification material) must come from environment variables or a managed secret store, never source control.
- Actuator endpoint exposure must be explicitly restricted before any shared deployment — liveness/readiness may be public, `metrics`/`env`/`loggers` must not be.

`feature/security` is the recommended next branch: replace `config.SecurityConfig`'s placeholder with real JWT resource-server validation and `@PreAuthorize` on every endpoint per TDS 43's authorization matrix.

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

**Near-term:** `feature/security` (real JWT authentication, `@PreAuthorize` authorization), bulk transaction ingestion, transaction retrieval/search, OpenAPI/Swagger generation.

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
feature/categorisation → feature/audit → feature/transaction → feature/aggregation → feature/api
```

All branches above are complete. **`feature/security` is the recommended next branch** — see [Security](#security).

`feature/project-structure` established only the package skeleton, Spring Modulith module boundaries, architecture verification tests, and shared configuration structure — no business logic. `categorisation` and `audit` were built before `transaction` because the transaction ingestion workflow depends on both (assigning a category and recording an audit event are part of processing a transaction, not features bolted on afterwards). `aggregation` came after `transaction` because it only reads transaction data that must already exist. `api` came last because it wires already-completed use cases to HTTP without introducing new business behaviour. See [`CLAUDE.md`](CLAUDE.md#implementation-rules) for the full rationale. Before implementing a feature:

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
