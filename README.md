# Transaction Aggregation API

A modular-monolith Spring Boot service that ingests financial transactions from multiple upstream sources, validates and categorises them, and exposes REST APIs for retrieval and financial aggregation.

> **Status:** All planned bounded contexts are implemented and secured (`project-structure` → `shared` → `customer` → `merchant` → `categorisation` → `audit` → `transaction` → `aggregation` → `api` → `security`), plus three post-MVP branches: `feature/transaction-query` (transaction retrieval and search), `feature/transaction-bulk` (bulk transaction creation with partial success), and `feature/category-admin` (category/categorisation-rule administration — see [Category & Rule Administration](#category--rule-administration) for the explicit scope decisions this last branch required, since SAD/TDS document it only partially). The REST API is wired up, authenticated via JWT bearer tokens, and authorized per SAD 36.4/TDS 42/ADR-016, tested end-to-end. The SAD, accepted ADRs, and TDS in [`documentation/`](documentation/) remain the source of truth. See [Development Workflow](#development-workflow) for the delivery order and current branch status.

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

Security → Shared   (JWT validation, role/authority mapping, access-denied handling; no business module depends on it)
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
| Security | Spring Security + OAuth2 Resource Server (JWT bearer, RS256, issuer/audience validation) — see [Security](#security) |
| Validation | Spring Validation (Jakarta Bean Validation) |
| Observability | Spring Boot Actuator, Micrometer, Prometheus registry |
| Build | Maven (via `mvnw` wrapper) |
| Containers (dev) | Docker Compose, Spring Boot Docker Compose support |
| Testing | JUnit 5, Mockito, Testcontainers (PostgreSQL), Spring Boot Test (incl. `@WebMvcTest`), Spring Modulith Test |

See [`Solution_Architecture_Document(SAD)_v_2_Part_6B_Governance_and_Reference.md`](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_6B_Governance_and_Reference.md) 52 for the full, versioned technology decision list and rationale (ADRs).

## Implemented Capabilities

- Single transaction ingestion (`POST /api/v1/transactions`): validation, duplicate detection (application pre-check + database unique-constraint fallback), merchant resolution, rule-based categorisation, persistence, and an audit event — all within one transactional use case.
- Bulk transaction ingestion (`POST /api/v1/transactions/bulk`, up to 500 items): each item processed independently through the same single-create use case, in its own transaction, so one invalid item never rolls back another already-committed item — see [Bulk Transaction Creation](#bulk-transaction-creation) for the full partial-success/duplicate/error semantics.
- Transaction retrieval by id (`GET /api/v1/transactions/{id}`) and filtered, paginated, sorted transaction search (`GET /api/v1/transactions`) — customer/source/category/merchant/direction/status/date-range filters, sorting restricted to the one documented sortable field (`transactionTimestamp`), enrichment done via batch (not per-row) cross-module lookups to avoid N+1.
- Configurable, priority-ordered categorisation rules (merchant and description matching) with a seeded fallback category.
- Category and categorisation-rule administration (`CATEGORY_ADMIN`): read-only category listing/lookup, plus full categorisation-rule create/update (activation, deactivation, and priority changes are all ordinary field edits through the same update endpoint) — see [Category & Rule Administration](#category--rule-administration) for the exact scope and the explicit decisions it required.
- Merchant name normalisation and find-or-create resolution.
- Customer, category, merchant, and monthly financial summaries (`GET /api/v1/customers/{customerId}/{summary|categories|merchants|monthly-summary}`), computed read-only from persisted transaction data over a caller-supplied date range.
- Append-only audit trail for business-significant events (transaction created, validation failed, duplicate rejected, source/customer not found), keyed by correlation ID.
- Correlation-ID propagation: honours a client-supplied `X-Correlation-ID`, generates one when absent, returns it on every response (success or error), threads it through to audit events, and places it in the logging MDC for the duration of the request.
- RFC 9457 (`application/problem+json`) error responses with an `errorCode`/`correlationId`/`timestamp` extension shape, covering every documented failure scenario for the implemented endpoints, including authentication/authorization failures.
- JWT bearer authentication (OAuth2 Resource Server) and `@PreAuthorize`-enforced, fine-grained authority checks on every implemented endpoint, per the approved role-to-authority mapping (ADR-016) — see [Security](#security).

**Not yet implemented** (documented in the SAD/TDS but out of scope so far — see [`CLAUDE.md`](CLAUDE.md) for the exact exclusion rationale):

- Any customer/merchant/categorisation-admin/audit CRUD or read endpoint (no documented HTTP contract, or no backing use case, for any of these today) — the authorities for them (`CUSTOMER_READ`, `CATEGORY_ADMIN`, `AUDIT_READ`, `OPERATIONS_READ`) are defined in the approved mapping but have nothing to attach to yet.
- OpenAPI/Swagger generation.
- Actuator liveness/readiness probes (only the default `/actuator/health` is exposed today) and restricting `/actuator/metrics`/`/env`/`/loggers`.

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
│   └── db/migration/               V1–V11 (customers, merchants, categorisation, audit, transactions + seeds,
│                                    transaction-search composite indexes)
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
| `security` | *(none)* | JWT resource-server configuration, role→authority mapping, RFC 9457-shaped 401/403 responses. Only `SecurityConfig` is public; its collaborators are package-private. |
| `config` | — | Cross-cutting, security-independent configuration: `ClockConfig`, `CorrelationIdFilter`. |

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

The application starts on the default port `8080` under the `transaction-aggregation-api` application name. **The application will fail to start unless `JWT_ISSUER_URI` is set** (or the `local` profile is active) — see [Security](#security) for the local-development JWT setup.

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
| `V11` | Composite indexes for transaction search: `(category_id, occurred_at)`, `(merchant_id, occurred_at)`, replacing the prior single-column indexes |

Applied migrations are immutable; schema changes are always additive new migration files. See [Part 4 – Domain & Data, 29.5](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_4_Domain_and_Data.md) for full rules.

## Testing

```bash
./mvnw test       # unit + integration tests (starts Testcontainers PostgreSQL — Docker must be running)
./mvnw verify      # full build-verification lifecycle
```

The test stack uses JUnit 5, Mockito, Spring Boot Test (including `@WebMvcTest` slices for controllers, mocking the use-case layer — no database needed there), Spring Security Test (`jwt()` `MockMvc` request post-processor — no real signed token or identity provider needed in any test), Spring Modulith's test starter (module boundary verification), and Testcontainers for real PostgreSQL integration tests — no mocked database in repository-layer tests. One `@SpringBootTest` test class (`CreateTransactionEndToEndTests`) exercises the full real stack (real Postgres, real correlation-ID filter, real JWT-secured filter chain, real controller, real persistence) for the create-transaction happy path, including asserting the JWT subject reaches the persisted audit row, plus a bulk-create scenario asserting a mixed-outcome `207` response and that both the created and duplicate-rejected items produce their expected audit rows — to catch wiring mistakes a mocked-use-case slice test cannot. A second class, `CategoryAdminEndToEndTests`, proves a category-admin write is genuinely visible to runtime categorisation: create a rule through the real admin API, then submit a real transaction and assert it was categorised by that new rule; a second scenario deactivates a rule and confirms the next transaction no longer matches it. `JpaCategorisationRuleRepositoryAdapterConcurrencyTests` is a dedicated, non-`@Transactional` (`Propagation.NOT_SUPPORTED`) persistence test proving the optimistic-lock guarantee against two genuinely independent, separately-committing calls, not two calls sharing one rolled-back test transaction. Every `@SpringBootTest`/`@WebMvcTest` that loads `security.SecurityConfig` mocks the `JwtDecoder` bean (`@MockitoBean`) purely to avoid a startup-time network call or a real issuer dependency — actual authentication in tests is driven by `jwt()`, not the decoder. Testcontainers-based tests provision their own PostgreSQL container via `TestcontainersConfiguration` and do not depend on, or interact with, the `compose.yaml` database described under [Docker](#docker) — the two are independent container lifecycles, and Docker must be running for either. The testing pyramid, required test types per layer, and the required test list are defined in [Part 6A – Operations, 45](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_6A_Operations.md) and [TDS 70](documentation/Transaction_Aggregation_API_Technical_Design_Specification.md#70-recommended-first-implementation-slice).

## API Documentation

The API is versioned under `/api/v1`. Every endpoint below requires a valid JWT bearer token and the listed authority — see [Security](#security). OpenAPI/Swagger generation (`springdoc-openapi`) is specified in the architecture but remains deliberately deferred.

| Endpoint | Method | Required authority | Description |
|---|---|---|---|
| `/api/v1/transactions` | `POST` | `TRANSACTION_WRITE` | Create a single transaction. `201` + the created resource (`Location` header), `409` on duplicate, `404` on unknown source/customer, `400` on validation failure, `401`/`403` on auth failure. |
| `/api/v1/transactions/bulk` | `POST` | `TRANSACTION_WRITE` | Create up to 500 transactions, each processed independently. `207` with a per-item outcome envelope for any request that reaches processing (all-success, all-failure, or mixed), `400` if the batch is empty/oversized/malformed, `401`/`403` on auth failure. See [Bulk Transaction Creation](#bulk-transaction-creation). |
| `/api/v1/transactions/{id}` | `GET` | `TRANSACTION_READ` | Retrieve a single transaction by id, fully enriched with merchant/category data. `200`, `404` if unknown, `401`/`403` on auth failure. |
| `/api/v1/transactions` | `GET` | `TRANSACTION_READ` | Filtered, paginated, sorted transaction search — `customerId`/`sourceCode`/`categoryCode`/`merchantId`/`direction`/`status`/`occurredFrom`/`occurredTo`/`page`/`size`/`sort` query params; `sort` accepts only `transactionTimestamp` (asc/desc). `200` with a paginated envelope, `400` on any validation failure, `401`/`403` on auth failure. |
| `/api/v1/customers/{customerId}/summary` | `GET` | `AGGREGATION_READ` | Customer income/expenditure/net-cash-flow summary over `?from=&to=` (ISO dates). |
| `/api/v1/customers/{customerId}/categories` | `GET` | `AGGREGATION_READ` | Debit totals grouped by category over the same date range. |
| `/api/v1/customers/{customerId}/merchants` | `GET` | `AGGREGATION_READ` | Debit totals grouped by merchant over the same date range. |
| `/api/v1/customers/{customerId}/monthly-summary` | `GET` | `AGGREGATION_READ` | Income/expenditure/net-cash-flow grouped by month over the same date range. |
| `/api/v1/categories` | `GET` | `CATEGORY_ADMIN` | List every category. `200`, `401`/`403` on auth failure. Read-only — see [Category & Rule Administration](#category--rule-administration). |
| `/api/v1/categories/{id}` | `GET` | `CATEGORY_ADMIN` | Get a single category by id. `200`, `404` `CATEGORY_NOT_FOUND`, `401`/`403`. |
| `/api/v1/categorisation-rules` | `GET` | `CATEGORY_ADMIN` | List every categorisation rule (active and inactive). `200`, `401`/`403`. |
| `/api/v1/categorisation-rules/{id}` | `GET` | `CATEGORY_ADMIN` | Get a single rule by id, including its current `version`. `200`, `404` `RULE_NOT_FOUND`, `401`/`403`. |
| `/api/v1/categorisation-rules` | `POST` | `CATEGORY_ADMIN` | Create a rule (always starts `active: true`). `201` + the created resource (`Location` header), `400` on validation failure, `404` `CATEGORY_NOT_FOUND` for an unknown `categoryId`, `401`/`403`. |
| `/api/v1/categorisation-rules/{id}` | `PUT` | `CATEGORY_ADMIN` | Full-replacement update — every mutable field required, including `active` and `expectedVersion`. `200`, `400`, `404` `RULE_NOT_FOUND`/`CATEGORY_NOT_FOUND`, `409` `OPTIMISTIC_LOCK_CONFLICT` on a stale `expectedVersion`, `401`/`403`. |
| `/actuator/health` | `GET` | *(public)* | Default Boot health endpoint; the only actuator surface exposed today. |

The create-transaction response follows SAD 35.1's shape (including nested `merchant`/`category` objects), which is the authoritative source over TDS 28's narrower documented shape — see [`CLAUDE.md`](CLAUDE.md#documentation-precedence) for how documentation conflicts are resolved. The full, authoritative API contracts (request/response payloads, status codes, filtering, pagination, sorting for the not-yet-implemented endpoints) are documented in [Part 5 – API & Security, 34–35](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_5_API_and_Security.md) and [TDS Part 6](documentation/Transaction_Aggregation_API_Technical_Design_Specification.md#part-6--api-contract). The category/rule administration endpoints are a partial exception to "documented" above — see [Category & Rule Administration](#category--rule-administration).

All errors use RFC 9457 Problem Details (`application/problem+json`) with a stable `errorCode`, `correlationId`, and `timestamp`. Error codes match [SAD 39.4](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_5_API_and_Security.md) exactly (`TRANSACTION_DUPLICATE`, `SOURCE_NOT_FOUND`, `CUSTOMER_NOT_FOUND`, `CATEGORY_NOT_FOUND`, `TRANSACTION_NOT_FOUND`, `REQUEST_VALIDATION_FAILED`, `INVALID_DATE_RANGE`, `OPTIMISTIC_LOCK_CONFLICT`, `AUTHENTICATION_REQUIRED`, `TOKEN_INVALID`, `ACCESS_DENIED`, `INTERNAL_SERVER_ERROR`) — a real SAD/TDS naming conflict was found and resolved here in `feature/security` (TDS 40 uses a different `TRX-NNN`/`SEC-NNN` scheme; SAD outranks TDS per [documentation precedence](CLAUDE.md#documentation-precedence)). `RULE_NOT_FOUND` (404) is the one genuinely new code in this API, added in `feature/category-admin` because no rule-specific not-found code exists in the SAD 39.4 catalogue. One documented gap remains: `TransactionValidationException` still maps to a generic `REQUEST_VALIDATION_FAILED` code rather than TDS 40's specific `TRX-002`/`TRX-004`/`TRX-005` codes, since the exception doesn't yet carry which invariant failed — giving it a structured reason is deferred work.

## Bulk Transaction Creation

`POST /api/v1/transactions/bulk` (FR-02, UC-02, SAD 32.5/35.2/39.8, TDS 29) accepts a wrapper request body, `{ "transactions": [ <the same per-item shape as POST /api/v1/transactions>, ... ] }`, of 1–500 items.

**Partial success, not all-or-nothing.** Each item is processed independently, in its own database transaction, by calling the same `CreateTransactionUseCase` the single-create endpoint uses — reused, not duplicated. There is no transaction around the batch as a whole: when one item fails, its own transaction rolls back cleanly and every previously-processed item stays committed exactly as it was, because each ran as its own already-completed, independent transaction. Processing is strictly sequential (no parallel/async processing) — this is also what lets a duplicate `externalTransactionId` appearing twice in the *same* batch be caught by the ordinary duplicate-detection path (the first occurrence commits before the second is attempted), with no separate same-batch pre-scan needed.

**Response — always `207 Multi-Status`** for any request that reaches per-item processing, whether every item succeeded, every item failed, or the outcome was mixed (SAD 39.3 documents exactly one bulk-specific status):
```json
{
  "total": 3,
  "successful": 2,
  "failed": 1,
  "results": [
    { "index": 0, "status": "CREATED", "transactionId": "9f9008dc-..." },
    { "index": 1, "status": "CONFLICT", "errorCode": "TRANSACTION_DUPLICATE", "detail": "..." },
    { "index": 2, "status": "CREATED", "transactionId": "da74fc23-..." }
  ]
}
```
Results preserve submission order. `status` is one of `CREATED`, `CONFLICT`, `FAILED` — the only vocabulary SAD 35.2's own example and SAD 39.8 support (39.8 defines no `status` field or enumeration at all); every failure other than a duplicate is reported as `FAILED`, with `errorCode` (the same SAD 39.4 catalogue used everywhere else in this API — `TRANSACTION_DUPLICATE`, `CUSTOMER_NOT_FOUND`, `SOURCE_NOT_FOUND`, `REQUEST_VALIDATION_FAILED`) carrying the specific reason. No new bulk-only error codes exist.

**A whole-request `400`** (not a 207) is returned only for a genuinely malformed envelope — an empty `transactions` list, more than 500 items, or a `null` element — before any item is processed at all.

**Only expected, per-item business/validation failures become a 207 result.** `TransactionValidationException` (covering both a missing required field on the raw item and every existing single-create business-invariant check — amount positivity, currency format, direction validity, and so on), `CustomerNotFoundException`, `TransactionSourceNotFoundException`, and `DuplicateTransactionException` are caught explicitly, one per item. An unexpected/systemic failure (e.g. the database becoming unavailable partway through the batch) is **not** caught — it propagates and aborts the whole request with the normal whole-request `500`/`503` Problem Details response, not a disguised per-item `FAILED` result; already-processed items remain committed, and the client must reconcile via `GET /api/v1/transactions`.

**Audit** — unchanged and automatic: because bulk reuses the single-create use case per item, every item (success or failure) gets exactly the same audit event a standalone single-create call would produce, via the same `REQUIRES_NEW` audit transaction. No bulk-level audit event exists; none is documented.

**No schema change.** Bulk writes through the same `transactions` table/write path as single-create; no Flyway migration was needed.

## Category & Rule Administration

`feature/category-admin` closes the `CATEGORY_ADMIN` authority's documented purpose ("manage categories and rules", SAD 36.4), but neither SAD nor TDS documents a full contract for it — no HTTP method/path table, no request/response fields, no status codes, unlike every other implemented capability. **The scope below is an explicit project decision, not something fully specified by the documentation** — see [`CLAUDE.md`](CLAUDE.md#implementation-rules) for the detailed reasoning behind each choice.

**Categories are read-only.** `GET /api/v1/categories` (list) and `GET /api/v1/categories/{id}` (get) — no create, update, deactivate, or delete. This was the narrowest reading fully supported by the documentation's own named artifacts: TDS's request-DTO catalogue names `CreateCategorisationRuleRequest`/`UpdateCategorisationRuleRequest` but no category equivalent, and its response-DTO catalogue names only one read-shaped `CategoryResponse`.

**Categorisation rules support full create/update, no physical delete.** `GET /api/v1/categorisation-rules` (list, active and inactive), `GET /api/v1/categorisation-rules/{id}`, `POST /api/v1/categorisation-rules` (create — always starts `active: true`), `PUT /api/v1/categorisation-rules/{id}` (full-replacement update — every mutable field required in one request, including `active` and `categoryId`; there is no separate activate/deactivate/priority-change endpoint, since nothing documents these as distinct operations from an ordinary field edit). Duplicate rules (same match criteria) remain explicitly permitted, matching the existing seed data's own pattern of several rules sharing one priority. No physical delete exists for either resource: a category referenced by any rule or transaction is already undeletable at the database level (`RESTRICT` foreign keys, no cascade), and SAD 32.10's retention philosophy argues against deleting rules too — the existing `active` flag (persisted since `feature/categorisation`, unused by any write path until now) is the only "removal" mechanism.

**Optimistic locking.** Every rule read (`CategorisationRuleResponse`) carries a `version` field; every `PUT` must echo the version it was read at as `expectedVersion`. A stale `expectedVersion` returns `409 OPTIMISTIC_LOCK_CONFLICT` (an existing SAD 39.4 code, unused until now). `@Version` remains solely on the JPA entity — the domain aggregate and every application/API contract carry `version` only as a plain `long`, never a persistence type. The update path loads and mutates one single managed entity in one method call specifically to keep this guarantee correct under real concurrent writes — see [`CLAUDE.md`](CLAUDE.md#implementation-rules) for exactly why an earlier two-call draft was rejected during review, and `JpaCategorisationRuleRepositoryAdapterConcurrencyTests` for the two-genuinely-independent-transactions proof.

**Runtime categorisation is unaffected except by design.** `CategorisationRuleEngine`/`CategorisationService` are entirely untouched; a newly created, updated, or deactivated rule is picked up by the very next transaction categorisation purely because `findAllActive()` already re-reads from the database on every call — proved end-to-end by `CategoryAdminEndToEndTests` (create a rule via the admin API, then submit a real transaction through it; separately, deactivate a rule and confirm the next transaction no longer matches it).

**No audit events, no new migration, no caching.** Admin changes are not documented as audit-worthy anywhere, so none are recorded. `active`/`version` columns already existed on both tables since `feature/categorisation`'s own V3/V4 migrations — this branch only starts writing through schema that had been sitting ready.

## Security

The temporary permit-all posture from `feature/api` has been replaced with the real, documented model (SAD 36, TDS 41–44, ADR-007, ADR-016). Every endpoint requires a valid JWT bearer token; the previous default-auto-configuration risk (HTTP Basic with a random per-boot password) no longer applies either way, since a real `SecurityFilterChain` bean is always defined.

**Authentication** — OAuth2 Resource Server, JWT bearer tokens (`Authorization: Bearer <token>`), validated via Spring Security's standard `NimbusJwtDecoder`:
- Signature: RS256-restricted by default (`NimbusJwtDecoder`'s own default when built from a JWK set).
- Issuer, expiry/not-before: Spring's default validator chain (60s clock skew, no custom override).
- Audience: validated explicitly against `app.security.expected-audience` — Spring's default validator chain does not check `aud` on its own, so this is added by hand in `security.SecurityConfig`.
- No custom token decoding, no custom cryptography anywhere in this codebase.

**Authorization** — `@PreAuthorize` on each controller method is the single source of authorization truth (`SecurityFilterChain`'s own `authorizeHttpRequests` only distinguishes public vs. authenticated, never repeats an authority check):
- **Authorities** protect individual operations (`TRANSACTION_READ`, `TRANSACTION_WRITE`, `CUSTOMER_READ`, `AGGREGATION_READ`, `CATEGORY_ADMIN`, `AUDIT_READ`, `OPERATIONS_READ` — SAD 36.4).
- **Roles** are named collections of authorities assigned to a client (`ROLE_API_CONSUMER`, `ROLE_SUPPORT`, `ROLE_ADMIN` — TDS 42, ADR-016). A JWT's `roles` claim (already `ROLE_`-prefixed) is expanded into granted authorities at authentication time by `security.RoleClaimAuthoritiesConverter`; role names are never checked directly, and an unrecognised role contributes no authorities rather than failing the request outright.
- See the endpoint table under [API Documentation](#api-documentation) for the exact authority each endpoint requires.

**Errors** — 401/403 responses use the same RFC 9457 shape as every other API error (`security.ProblemDetailAuthenticationEntryPoint`/`ProblemDetailAccessDeniedHandler`), including `correlationId`: `AUTHENTICATION_REQUIRED` (no token), `TOKEN_INVALID` (bad signature/expired/malformed — distinguished via Spring Security's own `BearerTokenError`, not custom parsing), `ACCESS_DENIED` (authenticated but missing the required authority).

**Actor propagation** — the JWT `sub` claim is read via `java.security.Principal.getName()` in `TransactionController` (a JDK type, not a Spring Security one) and flows into `CreateTransactionCommand.actor()`, replacing the earlier `"SYSTEM"` placeholder in audit records. `transaction.application` never imports a Spring Security or JWT type.

**Session/CSRF/CORS** — stateless (`SessionCreationPolicy.STATELESS`), CSRF disabled (no cookie-based session to protect), no CORS configuration (nothing documents a browser-based client — SAD 36.12 says disabled by default in that case).

**Local development** — no external identity provider is documented or required. Run with `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` to activate a local-only symmetric-key `JwtDecoder` (`security.SecurityConfig`'s `local`-profile bean), then mint a matching HS256 test token (e.g. via [jwt.io](https://jwt.io)'s debugger) signed with the published local-only key `local-only-test-signing-key-not-a-real-secret-32bytes-minimum`, including a `roles` claim such as `["ROLE_ADMIN"]`. This key is not a secret and is never used outside the `local` profile. Automated tests don't need this at all — they use Spring Security Test's `jwt()` `MockMvc` request post-processor to construct an already-authenticated principal directly.

**Configuration** — `spring.security.oauth2.resourceserver.jwt.issuer-uri` has **no default value** in the main configuration: an unset `JWT_ISSUER_URI` environment variable makes the application **fail to start**, rather than silently accepting tokens from any/no issuer. No concrete external identity provider is documented anywhere in the SAD/TDS/ADRs, so none is hardcoded — this must be supplied per deployment.

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=${JWT_ISSUER_URI}
app.security.expected-audience=${JWT_EXPECTED_AUDIENCE:transaction-aggregation-api}
```

- HTTPS is mandatory outside local development; secrets (DB credentials, JWT signing/verification material) must come from environment variables or a managed secret store, never source control.
- Actuator endpoint exposure must be explicitly restricted before any shared deployment — today only the default `/actuator/health` is exposed at all (no other actuator endpoint is configured), and it is the one endpoint permitted without authentication, per SAD 36.10.

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

**Near-term:** OpenAPI/Swagger generation, actuator liveness/readiness probes and restricting non-health actuator endpoints, a structured reason on `TransactionValidationException` (to reach TDS 40's specific `TRX-002`/`TRX-004`/`TRX-005` codes instead of the current generic `REQUEST_VALIDATION_FAILED`), a focused Testcontainers concurrent-duplicate-write test against `JpaTransactionRepositoryAdapter` (considered during `feature/transaction-bulk` and deliberately deferred, since bulk itself introduces no new concurrency), category create/update/deactivate/delete (deliberately excluded from `feature/category-admin` pending a project decision — see [Category & Rule Administration](#category--rule-administration)), rule physical deletion, admin audit events.

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
feature/categorisation → feature/audit → feature/transaction → feature/aggregation →
feature/api → feature/security
```

Every branch in the original sequence is now complete, plus three post-MVP branches: `feature/transaction-query`, adding transaction retrieval/search (`GET /api/v1/transactions/{id}`, `GET /api/v1/transactions` — FR-08, UC-03, UC-04, TDS 30-31); `feature/transaction-bulk`, adding bulk transaction creation with partial success (`POST /api/v1/transactions/bulk` — FR-02, UC-02, SAD 32.5/35.2/39.8, TDS 29); and `feature/category-admin`, adding category/categorisation-rule administration on top of the already-secured API — the first branch whose scope was not fully derivable from SAD/TDS and required explicit project decisions instead (see [Category & Rule Administration](#category--rule-administration)).

`feature/project-structure` established only the package skeleton, Spring Modulith module boundaries, architecture verification tests, and shared configuration structure — no business logic. `categorisation` and `audit` were built before `transaction` because the transaction ingestion workflow depends on both (assigning a category and recording an audit event are part of processing a transaction, not features bolted on afterwards). `aggregation` came after `transaction` because it only reads transaction data that must already exist. `api` wired already-completed use cases to HTTP without introducing new business behaviour, behind a temporary permit-all posture. `feature/security` replaced that posture with the real, documented JWT/RBAC model last, since it needed real HTTP endpoints to secure. `feature/transaction-query` came after all ten, as the first post-MVP gap-analysis-driven branch, adding two new `TRANSACTION_READ`-protected read endpoints and the batch (not per-row) cross-module enrichment lookups they need. `feature/transaction-bulk` followed, adding the documented bulk-create capability by calling the existing single-create use case once per item — see [Bulk Transaction Creation](#bulk-transaction-creation) for its partial-success/duplicate/error semantics. See [`CLAUDE.md`](CLAUDE.md#implementation-rules) for the full rationale. Before implementing a feature:

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
