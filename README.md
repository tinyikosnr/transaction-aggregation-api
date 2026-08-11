# Transaction Aggregation API

A modular-monolith Spring Boot service that ingests financial transactions from multiple upstream sources, validates and categorises them, and exposes REST APIs for retrieval and financial aggregation.

> **Status:** All planned bounded contexts are implemented and secured (`project-structure` → `shared` → `customer` → `merchant` → `categorisation` → `audit` → `transaction` → `aggregation` → `api` → `security`), plus eight post-MVP branches: `feature/transaction-query` (transaction retrieval and search), `feature/transaction-bulk` (bulk transaction creation with partial success), `feature/category-admin` (category/categorisation-rule administration), `feature/audit-query` (audit-event search — see [Audit Event Query](#audit-event-query) for the explicit scope decisions this branch required, since SAD/TDS document its existence but not its contract), `feature/openapi-documentation` (generated OpenAPI/Swagger UI documentation — see [OpenAPI / Swagger Documentation](#openapi--swagger-documentation)), `feature/observability` (Micrometer/Prometheus metrics — see [Observability](#observability)), `feature/health-readiness` (liveness/readiness probes — see [Health & Readiness](#health--readiness)), and `feature/structured-logging` (Boot-native ECS structured JSON logging — see [Structured Logging](#structured-logging)). The REST API is wired up, authenticated via JWT bearer tokens, and authorized per SAD 36.4/TDS 42/ADR-016, tested end-to-end. The SAD, accepted ADRs, and TDS in [`documentation/`](documentation/) remain the source of truth. See [Development Workflow](#development-workflow) for the delivery order and current branch status.

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
| API documentation | springdoc-openapi 3.0.3 (OpenAPI 3.1 + Swagger UI) — see [OpenAPI / Swagger Documentation](#openapi--swagger-documentation) |
| Observability | Spring Boot Actuator, Micrometer, Prometheus registry — see [Observability](#observability) |
| Logging | SLF4J 2.0.18 + Logback 1.5.34, Spring Boot 4.1 native ECS structured JSON console logging — no third-party encoder — see [Structured Logging](#structured-logging) |
| Build | Maven (via `mvnw` wrapper) |
| Containers (dev) | Docker Compose, Spring Boot Docker Compose support |
| Testing | JUnit 5, Mockito, Testcontainers (PostgreSQL, Toxiproxy — the latter test-only, for the health/readiness outage-recovery proof), Spring Boot Test (incl. `@WebMvcTest`), Spring Modulith Test |

See [`Solution_Architecture_Document(SAD)_v_2_Part_6B_Governance_and_Reference.md`](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_6B_Governance_and_Reference.md) 52 for the full, versioned technology decision list and rationale (ADRs).

## Implemented Capabilities

- Single transaction ingestion (`POST /api/v1/transactions`): validation, duplicate detection (application pre-check + database unique-constraint fallback), merchant resolution, rule-based categorisation, persistence, and an audit event — all within one transactional use case.
- Bulk transaction ingestion (`POST /api/v1/transactions/bulk`, up to 500 items): each item processed independently through the same single-create use case, in its own transaction, so one invalid item never rolls back another already-committed item — see [Bulk Transaction Creation](#bulk-transaction-creation) for the full partial-success/duplicate/error semantics.
- Transaction retrieval by id (`GET /api/v1/transactions/{id}`) and filtered, paginated, sorted transaction search (`GET /api/v1/transactions`) — customer/source/category/merchant/direction/status/date-range filters, sorting restricted to the one documented sortable field (`transactionTimestamp`), enrichment done via batch (not per-row) cross-module lookups to avoid N+1.
- Configurable, priority-ordered categorisation rules (merchant and description matching) with a seeded fallback category.
- Category and categorisation-rule administration (`CATEGORY_ADMIN`): read-only category listing/lookup, plus full categorisation-rule create/update (activation, deactivation, and priority changes are all ordinary field edits through the same update endpoint) — see [Category & Rule Administration](#category--rule-administration) for the exact scope and the explicit decisions it required.
- Merchant name normalisation and find-or-create resolution.
- Customer, category, merchant, and monthly financial summaries (`GET /api/v1/customers/{customerId}/{summary|categories|merchants|monthly-summary}`), computed read-only from persisted transaction data over a caller-supplied date range.
- Append-only audit trail for business-significant events (transaction created, validation failed, duplicate rejected, source/customer not found), keyed by correlation ID, with a queryable search API (`AUDIT_READ`) — see [Audit Event Query](#audit-event-query) for the exact filters, pagination, and the explicit decisions this capability required.
- Correlation-ID propagation: honours a client-supplied `X-Correlation-ID`, generates one when absent, returns it on every response (success or error), threads it through to audit events, and places it in the logging MDC for the duration of the request.
- RFC 9457 (`application/problem+json`) error responses with an `errorCode`/`correlationId`/`timestamp` extension shape, covering every documented failure scenario for the implemented endpoints, including authentication/authorization failures.
- JWT bearer authentication (OAuth2 Resource Server) and `@PreAuthorize`-enforced, fine-grained authority checks on every implemented endpoint, per the approved role-to-authority mapping (ADR-016) — see [Security](#security).
- Generated OpenAPI 3.1 documentation (`springdoc-openapi`) and Swagger UI, covering all 11 business endpoints — enabled under the `local` profile only, disabled by default/production — see [OpenAPI / Swagger Documentation](#openapi--swagger-documentation).
- Micrometer/Prometheus metrics: built-in JVM/process/HTTP/connection-pool metrics plus a small, SAD-38.3-literal set of custom transaction business counters and one processing-duration timer, exposed at `/actuator/prometheus`/`/actuator/metrics` (`OPERATIONS_READ`) — see [Observability](#observability).
- Actuator liveness/readiness probes: `/actuator/health/liveness` (process health only) and `/actuator/health/readiness` (process health plus PostgreSQL reachability), both publicly accessible, no custom health code — see [Health & Readiness](#health--readiness).

**Not yet implemented** (documented in the SAD/TDS but out of scope so far):

- Any customer/merchant/categorisation-admin/audit CRUD or read endpoint (no documented HTTP contract, or no backing use case, for any of these today) — the authorities for them (`CUSTOMER_READ`, `CATEGORY_ADMIN`, `AUDIT_READ`) are defined in the approved mapping but have nothing to attach to yet. `OPERATIONS_READ` is the one exception — now protecting the metrics endpoints below.
- Restricting/exposing `/env`/`/loggers` (deliberately not exposed).

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

Each module's internal layering (`domain`, `application`, `port`, `persistence`, etc.) is listed in full below in [Package Responsibilities](#package-responsibilities). The full target package layout is defined in the Technical Design Specification: [Technical Design Specification, Part 1](documentation/Transaction_Aggregation_API_Technical_Design_Specification.md#part-1--project-structure-and-package-design).

## Package Responsibilities

Base package: `za.co.tinyiko.transactionaggregation`. Every module is fully populated (`domain`, `application`, `port`, `persistence`, `mapper`, plus module-specific packages) except `aggregation` (no `port`/`persistence`/`mapper` — it owns no data), and `security`/`config`/`shared` (cross-cutting only, no `domain`/`port`/`persistence`). The domain layer never depends on Spring, Jakarta EE, or any persistence/framework type — it expresses business rules in plain Java (records for value objects, plain classes for entities); framework annotations belong only in infrastructure/presentation layers.

**`api`** — shared presentation layer
- `controller` — `TransactionController` (create/createBulk/get/search), `AggregationController`, `CategoryAdminController` (categories: list/get; rules: list/get/create/update), `AuditEventController` (search only, package-private)
- `dto.request` — `CreateTransactionRequest`, `BulkCreateTransactionsRequest`, `CreateCategorisationRuleRequest`, `UpdateCategorisationRuleRequest`
- `dto.response` — `TransactionResponse`, `TransactionSearchResponse` (+ nested `PageInfo`), `TransactionSearchItemResponse`, `BulkTransactionResponse`, `BulkTransactionItemResponse`, `CustomerSummaryResponse`, `CategorySummaryResponse`, `MerchantSummaryResponse`, `MonthlySummaryResponse`, `CategoryResponse`, `CategorisationRuleResponse`, `AuditEventResponse`, `AuditEventSearchResponse` (+ nested `PageInfo`)
- `mapper` — `TransactionApiMapper`, `AggregationApiMapper`, `CategoryAdminApiMapper`, `AuditEventApiMapper` (the only API-layer mapper needing an `ObjectMapper` collaborator, for `eventData` JSON parsing)
- `advice` — `GlobalExceptionHandler` (RFC 9457 `ProblemDetail`; error codes match SAD 39.4, not TDS 40 — see [Documentation Precedence](#documentation-precedence))

**`transaction`** — owns `transactions`, `transaction_sources`
- `domain` — `Transaction`, `TransactionId`, `TransactionSource`, `TransactionSourceId`, `Money`, `TransactionDirection` (local to this module), `TransactionStatus`, `SourceStatus`
- `application` — `CreateTransactionUseCase`/`CreateTransactionCommand`/`CreateTransactionService`; `TransactionValidationException`, `TransactionSourceNotFoundException`, `CustomerNotFoundException` (this module's own), `DuplicateTransactionException`; `TransactionQueryPort` + `CustomerTransactionTotals`/`CategoryTransactionTotal`/`MerchantTransactionTotal`/`MonthlyTransactionTotal`/`TransactionQueryService`; `GetTransactionUseCase`/`TransactionDetails`/`TransactionNotFoundException`/`GetTransactionService`; `SearchTransactionsUseCase`/`TransactionSearchCriteria`/`TransactionSearchResultItem`/`PagedResult<T>`/`SearchTransactionsService`; `CreateTransactionsBulkUseCase`/`BulkCreateTransactionsCommand`/`BulkTransactionItemInput`/`BulkTransactionItemResult`/`BulkTransactionResult`/`BulkCreateTransactionsService`. Exposed cross-module via a package-level `@NamedInterface` (first needed when `aggregation` became its first cross-module reader).
- `port` — `TransactionRepositoryPort` (save, findBySourceIdAndExternalTransactionId); `TransactionSourceRepositoryPort` (findByCode); `TransactionSummaryRepositoryPort` (customerTotals/categoryTotals/merchantTotals/monthlyTotals, own internal row projections — never the `application`-layer result records, see [Persistence & Transaction Conventions](#persistence--transaction-conventions)); `TransactionSearchRepositoryPort` (findDetailById, search, own internal row/query types); `TransactionMetricsPort` (transactionReceived/Processed/Rejected/DuplicateRejected/categorisationFallback/startProcessingTimer — no Micrometer type crosses this interface) + `RejectionReason` (closed 3-value enum)
- `persistence` — `TransactionEntity`, `TransactionSourceEntity`, `SpringDataTransactionRepository`, `SpringDataTransactionSourceRepository`, `SpringDataTransactionSummaryRepository`, `SpringDataTransactionSearchRepository`, `TransactionSpecifications`, `JpaTransactionRepositoryAdapter`, `JpaTransactionSourceRepositoryAdapter`, `JpaTransactionSummaryRepositoryAdapter`, `JpaTransactionSearchRepositoryAdapter`
- `metrics` — `MicrometerTransactionMetrics` (the sole `TransactionMetricsPort` implementation; the only class in this module permitted to import `io.micrometer.*`)
- `mapper` — `TransactionMapper` (toDomain + toEntity), `TransactionSourceMapper` (toDomain only)

**`categorisation`** — owns `transaction_categories`, `categorisation_rules`
- `domain` — `TransactionCategory`, `TransactionCategoryId`, `CategorisationRule`, `CategorisationRuleId`, `Direction` (local to this module — see [Module Boundaries & Dependency Rules](#module-boundaries--dependency-rules)), `MatchField`, `MatchOperator`, `CategorisationRuleEngine`
- `application` — `CategoriseTransactionUseCase`/`CategorisationInput`/`CategorisationDecision` (crosses the module boundary as `String`/`UUID`/`boolean` only, never a domain type)/`CategorisationService`; `GetCategoryUseCase` (get, findIdByCode, getByIds) + `CategoryView`/`GetCategoryService`; `ListCategoriesUseCase` (list, get) + `CategoryAdminView` (deliberately distinct from `CategoryView`); `ListCategorisationRulesUseCase`, `GetCategorisationRuleUseCase`, `CategorisationRuleView` (carries `version` as a plain `long`), `CreateCategorisationRuleUseCase`/`CreateCategorisationRuleCommand`, `UpdateCategorisationRuleUseCase`/`UpdateCategorisationRuleCommand`, `CategorisationRuleAdminService`, `RuleNotFoundException`, `RuleConflictException`, `RuleValidationException`. Exposed via a package-level `@NamedInterface`.
- `port` — `CategoryRepositoryPort` (findFallback, findByCode, findByIds, findAll — read-only, no save); `CategorisationRuleRepositoryPort` (findAllActive returning `ActiveCategorisationRule`; findAll, findRowById, create, update returning this port's own `CategorisationRuleRow`); `ActiveCategorisationRule` (pairs a rule with whether its target category is the fallback category, via one JOIN, no extra query)
- `persistence` — `TransactionCategoryEntity`, `CategorisationRuleEntity`, `SpringDataTransactionCategoryRepository`, `SpringDataCategorisationRuleRepository`, `JpaCategoryRepositoryAdapter`, `JpaCategorisationRuleRepositoryAdapter`
- `mapper` — `TransactionCategoryMapper` (toDomain only — category writes stay out of scope), `CategorisationRuleMapper` (toDomain, toEntity, applyTo)

**`aggregation`** — owns no tables; reads via `transaction.application.TransactionQueryPort`
- `domain` — `DateRange` (from/to `LocalDate`, both inclusive, max 24-month span — TDS 20)
- `application` — `GetCustomerSummaryUseCase`, `GetCategorySummaryUseCase`, `GetMerchantSummaryUseCase`, `GetMonthlySummaryUseCase` (TDS 6's own four documented interfaces, never consolidated into one); `CustomerSummaryView`, `CategorySummaryView`, `MerchantSummaryView`, `MonthlySummaryView`; `GetCustomerSummaryService`, `GetCategorySummaryService`, `GetMerchantSummaryService`, `GetMonthlySummaryService`; `CustomerNotFoundException` (this module's own). No `port`/`persistence`/`mapper` package — there is nothing of its own to read or write.

**`customer`** — owns `customers`
- `domain` — `Customer`, `CustomerId`, `CustomerStatus`
- `application` — `CustomerLookupPort`, `CustomerExistsPort` (takes a raw `UUID`, not `CustomerId`), `RegisterCustomerUseCase`/`RegisterCustomerCommand`, `CustomerLookupService`, `CustomerRegistrationService`, `CustomerNotFoundException`, `DuplicateCustomerReferenceException`. Exposed via a package-level `@NamedInterface`.
- `port` — `CustomerRepositoryPort`
- `persistence` — `CustomerEntity`, `SpringDataCustomerRepository`, `JpaCustomerRepositoryAdapter`
- `mapper` — `CustomerMapper`

**`merchant`** — owns `merchants`
- `domain` — `Merchant` (immutable, no status), `MerchantId`, `MerchantNormaliser`
- `application` — `MerchantResolutionPort` (returns `MerchantResolutionResult`, not `Merchant`), `MerchantResolutionResult`, `MerchantResolutionService`, `DuplicateMerchantException`, `GetMerchantsUseCase` (get, findByIds), `MerchantView` (deliberately distinct from `MerchantResolutionResult` despite an identical shape — find-or-create vs. pure read), `GetMerchantsService`. Exposed via a package-level `@NamedInterface`.
- `port` — `MerchantRepositoryPort` (save, findByNormalisedName, findByIds)
- `persistence` — `MerchantEntity`, `SpringDataMerchantRepository`, `JpaMerchantRepositoryAdapter`
- `mapper` — `MerchantMapper`

**`audit`** — owns `audit_events` (append-only)
- `domain` — `AuditEvent` (immutable, append-only, no `@Version`), `AuditEventId`
- `application` — `RecordAuditEventUseCase` (returns `void`, not `AuditEvent`), `RecordAuditEventCommand`, `AuditService` (always its own `REQUIRES_NEW` transaction — see [Persistence & Transaction Conventions](#persistence--transaction-conventions)); `SearchAuditEventsUseCase`, `AuditEventSearchCriteria`, `AuditEventSearchResult` (its own dedicated paging result, not `transaction.application.PagedResult<T>` promoted to `shared`), `AuditEventView`, `AuditSearchValidationException`, `SearchAuditEventsService`. Exposed via a package-level `@NamedInterface`.
- `port` — `AuditRepositoryPort` (save only — write side); `AuditQueryRepositoryPort` (search only — a separate port from the write side); `AuditEventRow`, `AuditEventSearchQuery`, `AuditEventSearchPage` (port's own shapes, never the domain `AuditEvent`)
- `persistence` — `AuditEventEntity` (`eventData` mapped to JSONB via `@JdbcTypeCode(SqlTypes.JSON)`), `SpringDataAuditRepository` (write, `JpaRepository`-based), `JpaAuditRepositoryAdapter`, `AuditEventSpecifications`, `SpringDataAuditSearchRepository` (read-only — `Repository<AuditEventEntity, UUID>` + `JpaSpecificationExecutor`, deliberately not `JpaRepository`, so no save/delete is ever exposed on the search side), `JpaAuditQueryRepositoryAdapter`
- `mapper` — `AuditMapper` (toDomain + toEntity, not applyTo — every save is a brand-new row, never a mutation of an existing one)

**`security`** — JWT validation, role/authority extraction, access-denied handling
- `SecurityConfig` (the only public type): `SecurityFilterChain`, `@EnableMethodSecurity`, the production issuer-based `JwtDecoder` (`@Profile("!local")`) and the local symmetric-key one (`@Profile("local")`)
- `RoleClaimAuthoritiesConverter` — ADR-016's role→authority mapping (package-private, no Spring bean — constructed directly by `SecurityConfig`)
- `ProblemDetailAuthenticationEntryPoint`, `ProblemDetailAccessDeniedHandler` — RFC 9457 401/403 responses matching `api.advice.GlobalExceptionHandler`'s shape
- `ProblemDetailSupport` — shared response-writing helper for the two above (package-private)

**`config`** — cross-cutting, security-independent configuration
- `ClockConfig` — the one `Clock` bean
- `CorrelationIdFilter` — resolves/generates `X-Correlation-ID`, MDC, request attribute (`@Order(HIGHEST_PRECEDENCE)`, runs before the security filter chain)
- OpenAPI `Info`/security-scheme configuration

**`shared`** — reusable, business-rule-free abstractions only
- `event` — `DomainEventEnvelope<T>` (the common event wire format, TDS 45)
- `logging` — `CorrelationId` (request-tracing value object, SAD 34.10/37)

No shared exception hierarchy, `shared.validation`, or `shared.util` exists — each would currently have zero consumers (see [Module Boundaries & Dependency Rules](#module-boundaries--dependency-rules) below).

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
| `config` | — | Cross-cutting, security-independent configuration: `ClockConfig`, `CorrelationIdFilter`, `OpenApiConfig`. |

Ownership rules, dependency directions, and data-access rules: [Part 4 – Domain & Data, 31](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_4_Domain_and_Data.md) and [Part 3 – Architecture, 21](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_3_Architecture.md).

## Module Boundaries & Dependency Rules

- **Allowed dependency directions:** `transaction → categorisation`, `transaction → audit`, `transaction → merchant`, `transaction → customer`, `aggregation → transaction` (read-only), `aggregation → customer` (read-only). No dependency runs the other way, and never a cycle. `categorisation` depends on nothing but `shared` — despite the SAD listing "Merchant module" under its dependencies as a business-level statement, `categorisation` only ever receives merchant text as a plain `String`, never calling into `merchant`'s ports.
- **Cross-module contracts return/accept primitives (`String`, `UUID`) or types from the target module's own `application` package, never a `domain` type** — even indirectly, as a field on an otherwise-exposed `application` type. This was discovered, not designed upfront: it surfaced the moment `transaction` became the first real cross-module caller of `categorisation`/`audit`/`merchant`, and every affected contract was revised rather than also exposing the `domain` package — `CategorisationInput.direction` became a `String`, `CategorisationDecision`'s ids became `UUID`, `RecordAuditEventUseCase.record` became `void`, `MerchantResolutionPort.resolve` returns `MerchantResolutionResult` (`UUID merchantId, String displayName`) instead of the domain `Merchant`, and `CustomerExistsPort.exists` takes a raw `UUID`, not `CustomerId`.
- **Sharing a concept across two modules is not, by itself, a reason to promote it into `shared`.** `categorisation.domain.Direction` (`CREDIT`/`DEBIT`) stayed local to `categorisation` even once `transaction` needed an equivalent concept (`transaction.domain.TransactionDirection`) — the two are connected only by an explicit `.name()` string crossing the module boundary, never a shared type.
- **Within a single module, the dependency direction is `application → port → persistence`, never the reverse.** `transaction.port.TransactionSummaryRepositoryPort`/`TransactionSearchRepositoryPort` own their own internal row/query types (`CustomerTotalsRow`, `TransactionSearchRow`, etc.) rather than returning the public `transaction.application` result records directly, which would make `port` depend on `application` — backwards, and a real dependency-cycle risk. Verified by `TransactionArchitectureTests#portDoesNotReferenceApplication`.
- A module never accesses another module's JPA repository or persistence package directly — cross-module reads/writes go through the target module's `application` use-case interfaces or `port` query interfaces only; asynchronous reactions go through domain events.
- Controllers depend only on `application`-layer use-case interfaces, never repositories.
- Persistence entities never cross a module boundary or reach a controller — always mapped to/from domain types and DTOs at the edges.
- `aggregation` owns no persistent tables and never writes transaction data — it composes summaries by reading through `transaction`'s query port only.
- These rules are enforced as executable tests (`ApplicationModules.of(TransactionAggregationApiApplication.class).verify()` in `ModularityTests`), not left as documentation-only conventions — a failing module-verification test is treated the same as a failing compile.
- Modules are recognised by Spring Modulith's default package-based convention alone — a bare `package-info.java` is enough, no annotation required. An explicit `@ApplicationModule` annotation is added only where a rule genuinely can't be expressed any other way: `shared` (`type = OPEN, allowedDependencies = {}`), `categorisation` (`allowedDependencies = {}`), `audit` (`allowedDependencies = "shared"`), `transaction` (`allowedDependencies = {"customer :: application", "merchant :: application", "categorisation :: application", "audit :: application", "shared"}`), `aggregation` (`allowedDependencies = {"transaction :: application", "customer :: application"}` — no `merchant`, `categorisation`, `audit`, or `shared`, matching its genuinely narrower documented dependency list). `customer` and `merchant` carry no annotation — package-based detection already covers everything currently needed for them.
- **`allowedDependencies` entries that target a specifically-named interface must use the qualified `"module :: interfaceName"` syntax, not a bare module name.** A bare module name only grants access to that module's *default* (unnamed) interface, not every `@NamedInterface` the target module happens to expose — found the hard way when `transaction`'s `allowedDependencies` was first written with plain `"customer"`/`"merchant"`/`"categorisation"`/`"audit"` and `verify()` rejected every one of them, since each of those modules exposes its API via a `@NamedInterface` implicitly named after its own package (`"application"`), not the module's default interface.
- **Cross-module reads that enrich a collection of rows (e.g. a paginated search result) use a batch lookup contract, not a per-row call into another module.** `SearchTransactionsService` collects the distinct non-null merchant/category ids across a whole search page once, then calls `GetMerchantsUseCase.findByIds`/`GetCategoryUseCase.getByIds` exactly once each (skipped entirely when the page needs no enrichment) — both backed by a single `IN (...)` SQL query, never a loop of individual lookups.
- **An unresolvable filter code crossing a module boundary resolves to a guaranteed-non-matching sentinel, never to `null`.** `SearchTransactionsService.resolveSourceId`/`resolveCategoryId` substitute `UUID.randomUUID()` when a caller-supplied `sourceCode`/`categoryCode` doesn't resolve to a real id — `null` would be indistinguishable from "no filter requested" once it reaches persistence, silently turning an unknown-code filter into "match everything" instead of the correct empty result set.
- **Cross-module API exposure uses a package-level `@NamedInterface`, not per-class annotations.** Spring Modulith's default encapsulation only exposes a module's *root* package; nested packages like `categorisation.application` are internal and unreachable from other modules by default, even for public types. `categorisation.application`, `audit.application`, `customer.application`, `merchant.application`, and `transaction.application` each carry a single `@org.springframework.modulith.NamedInterface` on their `package-info.java`, exposing every public type in that package as one deliberate API surface, rather than annotating each exported type individually. `aggregation.application` has no `@NamedInterface` yet — nothing outside `aggregation` calls into it today (a future API-layer capability is its only documented future caller).

## Aggregate Ownership

| Aggregate root | Module | Notes |
|---|---|---|
| `Customer` | customer | Owns customer identity/status lifecycle |
| `Merchant` | merchant | Normalised merchant identity; immutable after creation, no status |
| `Transaction` | transaction | Immutable once successfully processed; sole writer of `transactions` |
| `TransactionSource` | transaction | Reference data for ingestion sources |
| `TransactionCategory`, `CategorisationRule` | categorisation | Two aggregate roots in one module |
| `AuditEvent` | audit | Append-only; no update or delete operations, ever |

Each module is the only writer of its own tables. Other modules read owned data only through the owning module's ports, never through direct table/repository access.

## Persistence & Transaction Conventions

**Ports and adapters.** Within each module, `port` defines outbound interfaces the domain/application layer needs; `persistence` provides the Spring Data JPA adapters that implement them. Inbound ports are the `application`-layer use-case interfaces controllers, or other modules, call.

- **Read-only ports need no `applyTo` mapper method or entity setters justified by production code.** `CategoryRepositoryPort`/`CategorisationRuleRepositoryPort` only define reads; `TransactionCategoryMapper`/`CategorisationRuleMapper`'s read-only side has only `toDomain`, no `applyTo`.
- **Append-only aggregates use a `toEntity` mapper method, not `applyTo`.** `applyTo(entity, domain)` exists to mutate an already-managed, possibly pre-existing JPA entity in place so Hibernate's dirty-checking and `@Version` increment stay correct on update. `AuditEvent` never has a pre-existing managed entity to look up and mutate — every `save` is a brand-new row — so `AuditMapper.toEntity(domain)` builds a fully-formed entity via its all-args constructor instead; `AuditEventEntity` correspondingly has no setters and no `@Version`.
- **A module can legitimately have no `port`, `persistence`, or `mapper` package at all**, when it genuinely owns no data — `aggregation` is the only current example; every number it returns comes from `transaction.application.TransactionQueryPort`.
- **A module can have more than one outbound port when read and write concerns are genuinely separate.** `transaction.port.TransactionSummaryRepositoryPort` is a second, distinct outbound port alongside `TransactionRepositoryPort` (and a third, `TransactionSearchRepositoryPort`) rather than bolting read-aggregate methods onto an already-focused write/duplicate-check port.
- **Database-side aggregation (`SUM`/`COUNT`/`GROUP BY` in SQL), not loading rows into memory to sum in Java.** `JpaTransactionSummaryRepositoryAdapter` computes every summary in SQL: JPQL `@Query` constructor-expression projections for three of its four queries, one native query for the fourth (month-grouping).
- **`COALESCE(SUM(...), 0)` is required, not defensive-only, wherever a query can legitimately return zero matching rows.** SQL's `SUM` over zero rows returns `NULL`, not zero — verified empirically against a real empty result set via Testcontainers, not assumed from SQL semantics alone.
- **Month-grouping needs a native query, and needs the timezone made explicit.** `monthlyTotals` uses `date_trunc('month', occurred_at AT TIME ZONE 'UTC')`, not bare `date_trunc('month', occurred_at)` — a first attempt using the bare form produced *wrong* results in testing, since PostgreSQL's `date_trunc` truncates in the current session's timezone, not UTC, bucketing a midnight-UTC transaction into the previous month.
- **Translating persistence exceptions.** When a persistence adapter's write can violate a database constraint the application layer needs to react to, the adapter catches the Spring/JDBC exception (`DataIntegrityViolationException`) and rethrows a domain/application exception from that module's `application` package (e.g. `JpaMerchantRepositoryAdapter` catching it and throwing `merchant.application.DuplicateMerchantException`) — application code never imports a Spring persistence exception type. The adapter must use `saveAndFlush`, not `save`, so the actual constraint check isn't deferred past the method's return. **Recovery is a per-case decision, not automatic just because the pattern matches:** `merchant` resolution recovers by re-querying and returning the concurrently-created row (find-or-create has no real difference between "found it first" and "someone else created it a moment earlier"); `transaction` deliberately does **not** recover the same way for the identical-looking race on `(transaction_source_id, external_transaction_id)` — BR-08 requires a duplicate to be *rejected* (HTTP 409), not silently resolved to the existing row.

**Transaction boundaries:**
- One HTTP write request executes within one application-level transaction, owned by the `application`-layer use case — never the controller, never the repository.
- Bulk ingestion processes each item in its own transaction boundary so a single invalid item never rolls back the rest of the batch.
- Long-running or external (network) calls never sit inside a database transaction.
- **Audit writes always run in their own independent transaction** (`@Transactional(propagation = Propagation.REQUIRES_NEW)` on `AuditService.record`), never inside the caller's transaction — an audit record must survive even when the operation it describes fails and rolls back its own transaction. `REQUIRES_NEW` suspends the caller's (possibly already rollback-marked) transaction and commits the audit write on a genuinely separate physical transaction/connection.
- Optimistic locking (`@Version`) protects mutable aggregates from lost updates; immutable processed transactions don't need update-oriented locking after creation. The domain aggregate itself never carries the version — it's a persistence-layer concern — and the mapper's job on save is to write the domain's field values onto the already-loaded, managed JPA entity (not construct a detached replacement), so Hibernate's own dirty-checking and version increment handle the locking correctly.

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

The test stack uses JUnit 5, Mockito, Spring Boot Test (including `@WebMvcTest` slices for controllers, mocking the use-case layer — no database needed there), Spring Security Test (`jwt()` `MockMvc` request post-processor — no real signed token or identity provider needed in any test), Spring Modulith's test starter (module boundary verification), and Testcontainers for real PostgreSQL integration tests — no mocked database in repository-layer tests. One `@SpringBootTest` test class (`CreateTransactionEndToEndTests`) exercises the full real stack (real Postgres, real correlation-ID filter, real JWT-secured filter chain, real controller, real persistence) for the create-transaction happy path, including asserting the JWT subject reaches the persisted audit row, plus a bulk-create scenario asserting a mixed-outcome `207` response and that both the created and duplicate-rejected items produce their expected audit rows — to catch wiring mistakes a mocked-use-case slice test cannot. A second class, `CategoryAdminEndToEndTests`, proves a category-admin write is genuinely visible to runtime categorisation: create a rule through the real admin API, then submit a real transaction and assert it was categorised by that new rule; a second scenario deactivates a rule and confirms the next transaction no longer matches it. `JpaCategorisationRuleRepositoryAdapterConcurrencyTests` is a dedicated, non-`@Transactional` (`Propagation.NOT_SUPPORTED`) persistence test proving the optimistic-lock guarantee against two genuinely independent, separately-committing calls, not two calls sharing one rolled-back test transaction. A third class, `AuditQueryEndToEndTests`, proves audit search is genuinely visible against real audit rows: submit a real transaction, then query `GET /api/v1/audit-events` by `correlationId` and by `aggregateType`/`aggregateId` and confirm the `TRANSACTION_CREATED` event is returned; a second scenario submits a duplicate and confirms `TRANSACTION_DUPLICATE_REJECTED` is discoverable the same way — no artificial direct-insert audit row is ever created. `AuditEventApiMapperTests` proves `eventData` serializes as real nested JSON rather than a double-encoded string. `ObservabilityEndToEndTests` proves the observability stack end-to-end against the real `MeterRegistry` bean: `/actuator/health` unchanged and public, `/actuator/prometheus`/`/actuator/metrics` requiring `OPERATIONS_READ` (401 unauthenticated, 403 wrong authority, 200 otherwise), a real transaction request incrementing `transactions.received`/`transactions.processed` by exactly one (before/after deltas, since the Spring context and its registry are shared across test methods), a real duplicate incrementing `transactions.duplicates` and not the generic `rejected` counter, and the scraped Prometheus text body containing both built-in and custom exported metric names. `MicrometerTransactionMetricsTests` uses a plain `SimpleMeterRegistry` (no Spring context) to prove the cardinality-safety policy directly: every meter's tags are swept for forbidden high-cardinality keys, and the `reason` tag on `transactions.rejected` is confirmed bounded to exactly the three `RejectionReason` values. `HealthReadinessEndToEndTests` proves the normal-state and security behaviour of the health/readiness endpoints against the shared Testcontainers PostgreSQL instance: `/actuator/health`/`/actuator/health/liveness`/`/actuator/health/readiness` all public and `UP`, readiness responses carrying no component detail, and metrics/Prometheus security unchanged by the new matchers. `HealthReadinessOutageRecoveryTests` is a separate, dedicated class (a genuinely different infrastructure topology, not reused from the shared suite container) that provisions its own isolated PostgreSQL container fronted by a Toxiproxy proxy on their own `Network`, routes the Spring context's real `DataSource` through that proxy from context startup, and empirically proves all three states against the same running context: reachable (liveness/readiness both `UP`), interrupted via `Proxy.disable()` (liveness stays `UP`, readiness `503`), and restored via `Proxy.enable()` (readiness returns to `UP`, using a small bounded retry since HikariCP's own pool-recovery discovery is asynchronous) — see [Health & Readiness](#health--readiness) for the full mechanism-selection reasoning. `StructuredLoggingEndToEndTests` drives real requests through the real filter chain and captures the real `ILoggingEvent`, encoded through the exact `CONSOLE` appender's encoder Spring Boot itself configured for `logging.structured.format.console=ecs` (not a `System.out` swap, which Logback's `ConsoleAppender` captures a reference to before a test could redirect it) — proving valid ECS JSON, correct `correlationId`/method/status/duration/route fields, the generated-id-matches-response-header case, and that a supplied bearer token never appears in the captured line. `CorrelationIdFilterTests` additionally proves the access-log event's field shape, `http.route` inclusion/omission (a matched Spring MVC route template only, never the raw request URI), and that a second, header-less request never inherits a prior request's correlation id. `GlobalExceptionHandlerTests` additionally proves the unexpected-exception path logs exactly one `ERROR` event carrying the real `Throwable`, while an expected business exception (e.g. a duplicate transaction) logs nothing — see [Structured Logging](#structured-logging) for the full design. Every `@SpringBootTest`/`@WebMvcTest` that loads `security.SecurityConfig` mocks the `JwtDecoder` bean (`@MockitoBean`) purely to avoid a startup-time network call or a real issuer dependency — actual authentication in tests is driven by `jwt()`, not the decoder. Testcontainers-based tests provision their own PostgreSQL container via `TestcontainersConfiguration` and do not depend on, or interact with, the `compose.yaml` database described under [Docker](#docker) — the two are independent container lifecycles, and Docker must be running for either. The testing pyramid, required test types per layer, and the required test list are defined in [Part 6A – Operations, 45](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_6A_Operations.md) and [TDS 70](documentation/Transaction_Aggregation_API_Technical_Design_Specification.md#70-recommended-first-implementation-slice).

## API Documentation

The API is versioned under `/api/v1`. Every endpoint below requires a valid JWT bearer token and the listed authority — see [Security](#security). A generated OpenAPI document and Swagger UI are also available under the `local` profile — see [OpenAPI / Swagger Documentation](#openapi--swagger-documentation).

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
| `/api/v1/audit-events` | `GET` | `AUDIT_READ` | Search audit events — `aggregateType`/`aggregateId`/`eventType`/`actor`/`correlationId`/`occurredFrom`/`occurredTo`/`page`/`size`/`sort` query params; `aggregateId` requires `aggregateType`; `sort` accepts only `occurredAt` (asc/desc). `200` with a paginated envelope, `400` on any validation failure, `401`/`403`. See [Audit Event Query](#audit-event-query). |
| `/actuator/health` | `GET` | *(public)* | Default Boot health endpoint; the only actuator surface exposed today. |

The create-transaction response follows SAD 35.1's shape (including nested `merchant`/`category` objects), which is the authoritative source over TDS 28's narrower documented shape — see [Documentation Precedence](#documentation-precedence) for how documentation conflicts are resolved. The full, authoritative API contracts (request/response payloads, status codes, filtering, pagination, sorting for the not-yet-implemented endpoints) are documented in [Part 5 – API & Security, 34–35](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_5_API_and_Security.md) and [TDS Part 6](documentation/Transaction_Aggregation_API_Technical_Design_Specification.md#part-6--api-contract). The category/rule administration and audit-query endpoints are a partial exception to "documented" above — see [Category & Rule Administration](#category--rule-administration) and [Audit Event Query](#audit-event-query).

All errors use RFC 9457 Problem Details (`application/problem+json`) with a stable `errorCode`, `correlationId`, and `timestamp`. Error codes match [SAD 39.4](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_5_API_and_Security.md) exactly (`TRANSACTION_DUPLICATE`, `SOURCE_NOT_FOUND`, `CUSTOMER_NOT_FOUND`, `CATEGORY_NOT_FOUND`, `TRANSACTION_NOT_FOUND`, `REQUEST_VALIDATION_FAILED`, `INVALID_DATE_RANGE`, `OPTIMISTIC_LOCK_CONFLICT`, `AUTHENTICATION_REQUIRED`, `TOKEN_INVALID`, `ACCESS_DENIED`, `INTERNAL_SERVER_ERROR`) — a real SAD/TDS naming conflict was found and resolved here in `feature/security` (TDS 40 uses a different `TRX-NNN`/`SEC-NNN` scheme; SAD outranks TDS per [Documentation Precedence](#documentation-precedence)). `RULE_NOT_FOUND` (404) is the one genuinely new code in this API, added in `feature/category-admin` because no rule-specific not-found code exists in the SAD 39.4 catalogue. One documented gap remains: `TransactionValidationException` still maps to a generic `REQUEST_VALIDATION_FAILED` code rather than TDS 40's specific `TRX-002`/`TRX-004`/`TRX-005` codes, since the exception doesn't yet carry which invariant failed — giving it a structured reason is deferred work.

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

`feature/category-admin` closes the `CATEGORY_ADMIN` authority's documented purpose ("manage categories and rules", SAD 36.4), but neither SAD nor TDS documents a full contract for it — no HTTP method/path table, no request/response fields, no status codes, unlike every other implemented capability. **The scope below is an explicit project decision, not something fully specified by the documentation.**

**Categories are read-only.** `GET /api/v1/categories` (list) and `GET /api/v1/categories/{id}` (get) — no create, update, deactivate, or delete. This was the narrowest reading fully supported by the documentation's own named artifacts: TDS's request-DTO catalogue names `CreateCategorisationRuleRequest`/`UpdateCategorisationRuleRequest` but no category equivalent, and its response-DTO catalogue names only one read-shaped `CategoryResponse`.

**Categorisation rules support full create/update, no physical delete.** `GET /api/v1/categorisation-rules` (list, active and inactive), `GET /api/v1/categorisation-rules/{id}`, `POST /api/v1/categorisation-rules` (create — always starts `active: true`), `PUT /api/v1/categorisation-rules/{id}` (full-replacement update — every mutable field required in one request, including `active` and `categoryId`; there is no separate activate/deactivate/priority-change endpoint, since nothing documents these as distinct operations from an ordinary field edit). Duplicate rules (same match criteria) remain explicitly permitted, matching the existing seed data's own pattern of several rules sharing one priority. No physical delete exists for either resource: a category referenced by any rule or transaction is already undeletable at the database level (`RESTRICT` foreign keys, no cascade), and SAD 32.10's retention philosophy argues against deleting rules too — the existing `active` flag (persisted since `feature/categorisation`, unused by any write path until now) is the only "removal" mechanism.

**Optimistic locking.** Every rule read (`CategorisationRuleResponse`) carries a `version` field; every `PUT` must echo the version it was read at as `expectedVersion`. A stale `expectedVersion` returns `409 OPTIMISTIC_LOCK_CONFLICT` (an existing SAD 39.4 code, unused until now). `@Version` remains solely on the JPA entity — the domain aggregate and every application/API contract carry `version` only as a plain `long`, never a persistence type. The update path loads and mutates one single managed entity in one method call specifically to keep this guarantee correct under real concurrent writes.

*Why one call, not two:* the first draft of `JpaCategorisationRuleRepositoryAdapter#update` split this into two port calls — `findRowById` (to compare `expectedVersion`) followed by a separate `update` call that re-loaded the entity — which review caught as a genuine race: if a concurrent write landed between those two loads, the second load would silently observe the newer version, the stale `expectedVersion` would then "coincidentally" match it, and the update would wrongly succeed. The fix, now in place: `update` takes `expectedVersion` directly and does the entire load → compare → mutate → flush sequence against one single managed entity, in one method, with no intervening second read. The explicit `entity.getVersion() != expectedVersion` check (comparing against that same just-loaded value) catches the common case — the caller's data was already stale before the write was even attempted. `saveAndFlush`'s own Hibernate-generated `UPDATE ... WHERE id = ? AND version = ?` (built from that identical entity instance, never a re-read one) is the final guard for a write landing in the narrow window between this method's own load and its flush, surfacing as `ObjectOptimisticLockingFailureException`. Both paths translate to the same `RuleConflictException` → `409 OPTIMISTIC_LOCK_CONFLICT`, proved by `JpaCategorisationRuleRepositoryAdapterConcurrencyTests` against two genuinely independent, separately-committing calls (`Propagation.NOT_SUPPORTED`), not two calls sharing one rolled-back test transaction.

**Runtime categorisation is unaffected except by design.** `CategorisationRuleEngine`/`CategorisationService` are entirely untouched; a newly created, updated, or deactivated rule is picked up by the very next transaction categorisation purely because `findAllActive()` already re-reads from the database on every call — proved end-to-end by `CategoryAdminEndToEndTests` (create a rule via the admin API, then submit a real transaction through it; separately, deactivate a rule and confirm the next transaction no longer matches it).

**No audit events, no new migration, no caching.** Admin changes are not documented as audit-worthy anywhere, so none are recorded. `active`/`version` columns already existed on both tables since `feature/categorisation`'s own V3/V4 migrations — this branch only starts writing through schema that had been sitting ready.

## Audit Event Query

`feature/audit-query` closes the `AUDIT_READ` authority's documented purpose (SAD 31.2: "Operational and compliance queries" — the *only* documented basis; SAD/TDS name no controller, no DTO, no filter list, no path). **The contract below is an explicit project decision, not something fully specified by the documentation.**

**One endpoint, read-only.** `GET /api/v1/audit-events` — no get-by-id (search already serves the realistic "show me everything about this request/aggregate" compliance workflow), no write/update/delete of any kind (audit stays append-only, structurally: the search-side Spring Data repository doesn't even have a `save`/`delete` method available to call).

**Filters:** `aggregateType`, `aggregateId`, `eventType`, `actor`, `correlationId`, `occurredFrom`/`occurredTo` — each a real persisted column, exact case-sensitive match, all independently optional except one rule: **`aggregateId` requires `aggregateType`** (the audited aggregate identity is the pair, not `aggregateId` alone; supplying it without `aggregateType` returns `400 REQUEST_VALIDATION_FAILED`). No `customerId`/`transactionId` aliases — a transaction's full audit trail is already reachable via `aggregateType=TRANSACTION&aggregateId=<id>`. Blank/empty-string filters are explicitly normalised to "absent" before the query is built, not left to incidental HTTP-binding behaviour.

**Pagination:** `page` default `0`, `size` default `20`, max `100`, always applied — mandatory capped pagination is the actual safety valve against unbounded queries here, not a date-range restriction. **Sorting:** `occurredAt` is the only sortable field (`asc`/`desc`), default `occurredAt,desc`. **No maximum date range** — deliberately different from transaction search's 24-month cap, since audit is a compliance surface where long historical lookback is a legitimate need.

**`eventData` is returned as real nested JSON, not an escaped string.** `event_data` is stored as JSONB and stays a raw `String` through `audit.domain`/`audit.application`/`audit.port` (never a JSON-library type below the API boundary); `api.mapper.AuditEventApiMapper` parses it exactly once, via the standard Boot-managed `ObjectMapper` (`objectMapper.readTree(...)`, not `@JsonRawValue`), into the `AuditEventResponse.eventData` field. A parse failure is not caught or downgraded to a string fallback — since the source is already-validated JSONB, a failure here means the persisted data itself is corrupt, so it propagates as a genuine `500`, never disguised as a client error.

**No cross-module enrichment.** An audit event response shows exactly what was captured at event time (`aggregateType`/`aggregateId`/`eventType`/`actor`/`correlationId`/`eventData`/`occurredAt`) — no live customer/transaction/category/merchant names are joined in, matching this codebase's `audit` module boundary (`allowedDependencies = "shared"` only, unchanged by this branch) and avoiding misrepresenting history when current business state has since changed.

**No new error code, no new migration.** Every validation failure reuses the existing `REQUEST_VALIDATION_FAILED`. All three query predicates (aggregate pair, `correlationId`, `occurredAt` range/sort) are already covered by the three indexes `feature/audit` originally created; `actor`/`eventType` alone stay unindexed, deliberately deferred pending real usage evidence rather than added speculatively against a write-heavy, append-only table.

## OpenAPI / Swagger Documentation

`feature/openapi-documentation` adds generated API documentation (SAD 35.9) via `springdoc-openapi-starter-webmvc-ui:3.0.3` — the first release published against Spring Boot 4.0.x, empirically verified (dependency resolution, `clean compile`, application context start, and a real `GET /v3/api-docs` call all confirmed passing) to also work cleanly against this project's Spring Boot 4.1.0, before any annotation work began.

**Endpoints:**
- `GET /v3/api-docs` — the generated OpenAPI 3.1 JSON document (also available as `/v3/api-docs.yaml`).
- `GET /swagger-ui.html` (redirects to `/swagger-ui/index.html`) — interactive Swagger UI.

**Enabled under `local` only, disabled by default/production** — an explicit project decision (not a documentation gap: SAD 35.9 says exposure "should be restricted or disabled in production according to deployment policy" without naming a mechanism). No portal, pipeline, or external consumer currently depends on either endpoint, so the conservative reading disables both by default:

```properties
# application.properties (default/production)
springdoc.api-docs.enabled=false
springdoc.swagger-ui.enabled=false

# application-local.properties (local profile only)
springdoc.api-docs.enabled=true
springdoc.swagger-ui.enabled=true
```

Run locally with `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` (see [Local development](#security) below) and browse `http://localhost:8080/swagger-ui.html`.

**Security** — `security.SecurityConfig` grants `permitAll()` to the five OpenAPI/Swagger paths unconditionally (the same treatment as `/actuator/health`), regardless of profile; whether they serve anything is controlled entirely by the `springdoc.*.enabled` properties above, not by the filter chain. This is deliberate: a disabled path under `permitAll()` reaches Spring MVC and returns a plain `404`, rather than a `401` that would otherwise leak "this path exists but you're not authenticated for it".

**Content** — one HTTP bearer/JWT security scheme (`bearerAuth`), applied globally; the five fine-grained authorities (`TRANSACTION_READ`, `TRANSACTION_WRITE`, `AGGREGATION_READ`, `CATEGORY_ADMIN`, `AUDIT_READ`) are documented as plain-text `@PreAuthorize` authorities in each operation's description, never modelled as OAuth2 scopes (this API validates externally-issued JWTs, it does not issue them). Four tags group the 11 documented endpoints: `Transactions`, `Aggregation`, `Category Administration`, `Audit`. `Info.version` is the literal external API contract version `"v1"` (matching the `/api/v1` path prefix), not the Maven artifact version.

**A genuine Jackson 3/Jackson 2 coexistence nuance, found empirically, not assumed:** springdoc 3.0.3 bundles its own internal Jackson 2 stack (via `swagger-core-jakarta`) for schema introspection, entirely separate from this project's own Jackson 3 REST serialization. `AuditEventResponse.eventData` (`tools.jackson.databind.JsonNode`, Jackson 3) is not recognised by springdoc's Jackson-2-based schema resolver as a JSON node type; declaring `@Schema(type = "object", implementation = Object.class)` on it avoids the misleading fallback (rendering it as a schema of type `"string"`) by instead producing an unconstrained "any value" schema (no `type` keyword — valid OpenAPI 3.1/JSON Schema for "any type"). This only affects the generated *schema documentation*; the real runtime response already embedded `eventData` as a genuine nested JSON object before this branch (proved by `AuditEventControllerTests`) and is unaffected.

**Scope boundary, enforced at build time** — `architecture.OpenApiArchitectureTests` scans every compiled class under the application's base package and fails the build if an OpenAPI annotation (`io.swagger.v3.oas.annotations.*`) appears anywhere outside `api.controller`, `api.dto`, or `config` — no domain, application, port, or persistence class in any module should ever need to import a Swagger type.

## Observability

`feature/observability` adds Micrometer/Prometheus metrics (SAD 38, FR-13's metrics portion). Scoped deliberately to metrics only — SAD 38.1 groups logs/metrics/traces/health under one "Observability Pillars" heading, but this branch delivers only the metrics pillar; structured JSON logging (closed by the later `feature/structured-logging`, see [Structured Logging](#structured-logging)), distributed tracing, and health/readiness/liveness probes (SAD 38.4) remain separate work (the latter closed by `feature/health-readiness`). `/actuator/health` is completely unchanged by this branch.

**Endpoints** (all profiles, not local-only — unlike Swagger, production scraping is the actual use case):

| Endpoint | Required authority | Purpose |
|---|---|---|
| `/actuator/health` | *(public, unchanged)* | Boot's default health endpoint. |
| `/actuator/prometheus` | `OPERATIONS_READ` | Prometheus-format scrape endpoint (`text/plain;version=0.0.4`). |
| `/actuator/metrics`, `/actuator/metrics/{name}` | `OPERATIONS_READ` | Human-oriented per-meter JSON drill-down (TDS 43: Admin-only). |

`management.endpoints.web.exposure.include=health,prometheus,metrics` — a narrow allow-list, never a wildcard; `/env`/`/loggers` and every other actuator endpoint stay unexposed (SAD 36.10). `OPERATIONS_READ` is the pre-existing authority already mapped to `ROLE_ADMIN` (SAD 36.4: "Access selected operational endpoints") — not a newly invented one, and previously unused by any endpoint until this branch. Enforced in the same `security.SecurityConfig` filter chain as every other path-level rule (no second, separately-ordered `SecurityFilterChain` for actuator paths).

**A genuine Spring Boot 4.1 behaviour difference from the plan's original assumption, found empirically via the autoconfiguration condition-evaluation report, not assumed:** `PrometheusMetricsExportAutoConfiguration` does **not** activate by default merely because `micrometer-registry-prometheus` is on the classpath and the endpoint is web-exposed — `@ConditionalOnEnabledMetricsExport` now resolves against `management.defaults.metrics.export.enabled`, which is effectively `false` with nothing else configured. `management.prometheus.metrics.export.enabled=true` opts in narrowly, per registry - not the global `management.defaults.metrics.export.enabled` switch, which would silently also enable every other registry (otlp/datadog/graphite/...) whose classes happen to be on the classpath.

**Built-in metrics (zero custom code):**
- **JVM/process/system** — `jvm.memory.*`, `jvm.gc.*`, `jvm.threads.*`, `process.cpu.usage`, `process.uptime`, `disk.*` — auto-bound by Boot once Actuator + a `MeterRegistry` exist (they already did; this branch only exposes them).
- **HTTP** — `http.server.requests`, tagged by `method`/`status`/`outcome`/`exception`/low-cardinality URI *template* (e.g. `/api/v1/transactions/{id}`, never the resolved id) — Boot's own default instrumentation, no manual controller instrumentation added anywhere.
- **Connection pool** — `hikaricp.connections.*` — auto-bound to the existing `HikariDataSource` bean by Boot's `DataSourcePoolMetricsAutoConfiguration`.

**Custom business metrics** — a small, SAD-38.3-literal set, owned entirely by `transaction` (`transaction.port.TransactionMetricsPort`, implemented by `transaction.metrics.MicrometerTransactionMetrics`). SAD 38.2's "Transaction Metrics" bullet list lives under one heading, not separate Transaction/Categorisation/Bulk sections, which is why no `categorisation`-owned metrics component exists — the one categorisation-related signal (fallback count) is recorded from `transaction`'s own port, using data `categorisation` computes and hands across the existing `CategorisationDecision` contract, never inferred from a UUID or hardcoded category code inside `transaction`. No bulk-specific counters exist either — `BulkCreateTransactionsService` calls the *injected* `CreateTransactionUseCase` bean once per item, so the single set of counters below already fires exactly once per item regardless of which HTTP endpoint it arrived through. No `audit`- or `aggregation`-owned metric exists: SAD 38 names none, and (for audit specifically) coupling to `AuditService`'s `REQUIRES_NEW` transaction/failure semantics wasn't worth taking on for a capability nothing documents:

| Java meter name | Exported Prometheus name | Semantic |
|---|---|---|
| `transactions.received` | `transactions_received_total` | Every ingestion attempt (single or bulk-item), before validation. |
| `transactions.processed` | `transactions_processed_total` | The transaction-creation workflow reached its completed normal success path, **including** successful success-audit recording — not a guarantee the enclosing database transaction has since physically committed (no `TransactionSynchronization`/`afterCommit` hook exists purely for metrics). |
| `transactions.rejected` (tag `reason`: `VALIDATION_FAILED`\|`SOURCE_NOT_FOUND`\|`CUSTOMER_NOT_FOUND`) | `transactions_rejected_total{reason="..."}` | One of the three non-duplicate rejection reasons. |
| `transactions.duplicates` | `transactions_duplicates_total` | Rejected as a duplicate (BR-08) — a separate counter from `rejected`, matching SAD 38.3. |
| `transactions.categorisation.fallback` | `transactions_categorisation_fallback_total` | A **successfully processed** transaction whose assigned category was the fallback category — computed by `categorisation` itself (`CategorisationDecision.fallbackApplied()`), not inferred from `matchedRuleId == null` (the seeded priority-1000 catch-all rule assigns the fallback category via a real rule match, so that would under-count). |
| `transactions.processing.duration` | `transactions_processing_duration_seconds_{count,sum,max}` | Timed around the whole `create(...)` attempt via try/finally — every outcome, including unexpected/systemic exceptions. |

Bulk ingestion needs no separate counters: `BulkCreateTransactionsService` calls the same `CreateTransactionUseCase` once per item, so every item — single or bulk — increments these counters exactly once, at exactly one emission point in the codebase.

**Cardinality policy** — no UUID, `correlationId`, `externalTransactionId`, `actor`, merchant name, or exception message is ever used as a tag; `reason` is a closed 3-value enum (`RejectionReason`). Enforced by a `SimpleMeterRegistry`-based unit test (`MicrometerTransactionMetricsTests`) that sweeps every registered meter for forbidden tag keys, and by `TransactionArchitectureTests`, which confirms no domain/application/port type in `transaction` imports `io.micrometer.*` (only the port interface itself is reflectable that way; the sole adapter, `MicrometerTransactionMetrics`, is package-private, the same reflection limitation already documented for every other package-private `@Service` in this codebase).

**Metrics are not audit.** No transactional coupling exists between metric recording and `AuditEvent` persistence; metric calls are plain, non-blocking, in-process `MeterRegistry` operations that can never cause a business operation to fail, and never replace the audit trail.

## Health & Readiness

`feature/health-readiness` closes FR-13's health/readiness portion and SAD §38.4 exactly: *"Liveness determines whether the process should be restarted... Readiness determines whether the application can accept traffic... Readiness should consider critical dependencies such as PostgreSQL. A temporary database failure may make the application unready without making it non-live."*

**Endpoints** (all public, all profiles):

| Endpoint | Semantic | Includes PostgreSQL? |
|---|---|---|
| `GET /actuator/health` | General aggregate status (all built-in contributors) | Yes (`db` is a top-level contributor) |
| `GET /actuator/health/liveness` | Is the process fundamentally alive and able to keep running? | **No** — must never restart-loop a healthy JVM over a database outage |
| `GET /actuator/health/readiness` | Should this instance currently receive traffic? | **Yes** — a database outage must stop traffic being routed here |

```bash
curl -s http://localhost:8080/actuator/health/liveness    # {"status":"UP"}
curl -s http://localhost:8080/actuator/health/readiness   # {"status":"UP"} or 503 {"status":"DOWN"}
```

**Spring Boot 4.1 already enables the `liveness`/`readiness` probe groups and their `/actuator/health/{liveness,readiness}` paths by default** — confirmed empirically (no `management.endpoint.health.probes.enabled` property exists anywhere in this project, and the groups are present regardless). The one gap the framework's own default leaves open — `db` is a registered health contributor but is **not** part of the `readiness` group by default — was proven with a real outage: stopping PostgreSQL made the plain `/actuator/health` `db` component (and the aggregate) go `DOWN`, while `/actuator/health/readiness` stayed `UP`. Closed with exactly one property:

```properties
management.endpoint.health.group.readiness.include=readinessState,db
```

No `management.endpoint.health.probes.enabled` (already the default), no explicit `liveness` group include (`livenessState`-only is already correct), no `diskSpace`/`ping`/`ssl` in either group — readiness answers "can this instance serve business traffic," not "is disk space healthy" or anything the application doesn't actually depend on.

**No custom code.** No `HealthIndicator`, no `HealthContributor`, no health controller, no DTO, no business-data validation in readiness (categorisation rules, seed data, etc. are database *content* invariants, not runtime dependency availability, and are deliberately not checked here). The built-in `db` contributor already uses the JDBC4 `Connection.isValid()` check — no custom `SELECT 1`.

**Public by design, minimal detail.** All three paths are `permitAll()` in the existing `security.SecurityConfig` (SAD 36.10 names the liveness/readiness sub-paths explicitly as publicly accessible; TDS 43 covers bare `/actuator/health` the same way) — no JWT, no new authority. `/actuator/metrics`/`/actuator/prometheus` remain `OPERATIONS_READ`-protected, unchanged. Confirmed empirically that the public liveness/readiness responses never include a `components`/database-detail breakdown, regardless of the main endpoint's own detail setting — public probes never leak database vendor/connection information.

**Spring Boot 4.1's graceful shutdown is enabled by default** (`server.shutdown` defaults to `graceful`, confirmed directly from the Boot 4.1.0 configuration metadata — not assumed from older Boot muscle memory) and is left completely untouched: no `server.shutdown` property is added, no shutdown-timeout tuning, no custom `AvailabilityChangeEvent` publishing. Readiness's transition to `REFUSING_TRAFFIC` during shutdown is entirely framework-managed.

**Outage/recovery proven empirically, including full recovery**, against a real, running application context — not the shared Testcontainers suite container (stopping that would destabilise every other test), but a dedicated, isolated PostgreSQL container fronted by a dedicated [Toxiproxy](https://github.com/Shopify/toxiproxy) container (`org.testcontainers:testcontainers-toxiproxy`, **test-only**, Boot-managed version), with the datasource routed through the proxy's stable host/port from the very first connection the application context ever opens:

1. **PostgreSQL reachable** — liveness `UP`, readiness `UP`.
2. **Connectivity interrupted at the proxy** (`Proxy.disable()` — chosen after empirically rejecting the higher-level `ContainerProxy.setConnectionCut(...)` helper, whose bytecode showed it installs a zero-bandwidth toxic that lets Toxiproxy *accept* a connection and then silently stall it forever, reproducing the same multi-minute hang this branch's own investigation hit when a real container was `pause()`d instead of `stop()`ped; `disable()` makes Toxiproxy stop *listening* instead — an immediate, clean rejection, empirically well under 100ms) — liveness stays `UP`, readiness returns `503 DOWN`.
3. **Connectivity restored** (`Proxy.enable()`) — liveness stays `UP`, readiness returns to `UP` (Hikari's own connection-pool recovery is asynchronous, not synchronous with `enable()`, so the test uses a small bounded retry rather than a single immediate assertion or a long fixed sleep).

## Structured Logging

`feature/structured-logging` closes ADR-011 and SAD 37 ("The application uses structured logging... Production logs should be emitted as JSON") using **Spring Boot 4.1's native Elastic Common Schema (ECS) structured-logging formatter — zero third-party dependency, zero `logback-spring.xml`**:

```properties
logging.structured.format.console=ecs
```

That single line is the entire configuration change. Confirmed empirically (decompiling the actual `spring-boot-4.1.0.jar` and running the application with real requests) that Boot 4.1 ships its own `org.springframework.boot.logging.logback.*` structured formatters — `logstash-logback-encoder` is not on the classpath and was never needed. ECS was chosen over Boot's also-native `logstash`/`gelf` presets because it natively supplies `service.name` (derived from `spring.application.name`, already set) and separates `error.type`/`error.message` from `error.stack_trace`, most directly matching SAD 37.1's field list. JSON is used in **every** profile, including `local` — a single, always-tested behaviour, rather than a fragile per-profile format override.

**A normal routed request** produces one console JSON line shaped like:
```json
{
  "@timestamp": "2026-08-11T09:15:37.088Z",
  "log": { "level": "INFO", "logger": "za.co.tinyiko.transactionaggregation.config.CorrelationIdFilter" },
  "process": { "pid": 11452, "thread": { "name": "http-nio-8080-exec-1" } },
  "service": { "name": "transaction-aggregation-api", "node": {} },
  "message": "Request completed",
  "correlationId": "f68017fa-2f98-4a52-a356-21fe41004965",
  "http": {
    "request": { "method": "GET" },
    "response": { "status_code": 404 },
    "route": "/api/v1/transactions/{id}"
  },
  "event": { "duration": 150553200 },
  "ecs": { "version": "8.11" }
}
```
Real, captured output — not a hand-authored example.

**`correlationId` reaches every log line automatically through MDC** — no second correlation-ID generator exists anywhere, and the MDC key/cleanup behaviour (`config.CorrelationIdFilter`, `X-Correlation-ID` → MDC `correlationId` → response header, removed in a `finally`) is completely unchanged from `feature/api`. Confirmed empirically that Spring Boot 4.1 includes MDC/context data in structured JSON **by default** — `logging.structured.json.context.include` is deliberately **not** set, since restating an already-active framework default adds nothing.

**Exactly two new SLF4J call sites in the whole codebase**, both infrastructure/presentation classes, never domain/application/port/persistence:
- **`config.CorrelationIdFilter`** — one `INFO` access-log event per request (SAD 37.1/37.3's "Correlation and Access Log Filter"), after `filterChain.doFilter(...)` returns: `http.request.method`, `http.response.status_code`, `event.duration` (elapsed **nanoseconds**, `System.nanoTime()`, matching ECS's documented unit — never wall-clock), and, only when available, `http.route`.
- **`api.advice.GlobalExceptionHandler`** — one `ERROR` event, exactly once, only for the generic/unexpected-exception handler (SAD 39: "Unexpected exceptions are logged once at the boundary"), passing the real `Throwable` so the ECS formatter serializes `error.type`/`error.message`/`error.stack_trace` natively. Every expected business/validation exception handler (duplicate, not-found, validation, conflict) logs nothing — each already has full traceability through its `errorCode`/`correlationId` and, for transaction failures, the audit trail; logging them again would duplicate that trail and spam low-value `ERROR` noise for ordinary client-driven outcomes.

**`http.route` is the matched Spring MVC route *template*, never the raw resolved path.** Read from `HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE`, not `request.getRequestURI()` — this application has path variables carrying real business identifiers (`/api/v1/transactions/{id}`, `/api/v1/customers/{customerId}/summary`), and logging the resolved path would leak a transaction/customer UUID "merely for convenience," contradicting the sensitive-data policy below. Empirically verified against a real running instance: a request to `/api/v1/transactions/<uuid>` logs `http.route=/api/v1/transactions/{id}`; a request rejected by Spring Security *before* any handler is resolved (e.g. an unauthenticated call to a protected endpoint) has no matching attribute at all, so `http.route` is **omitted entirely** — never a raw-URI fallback. No query string is ever logged.

**Sensitive-data policy (SAD 37.4)** — never logged, anywhere: the `Authorization` header, bearer/JWT token contents, passwords, database credentials, private keys/secrets, cookies, request/response bodies, query strings, complete transaction payloads, transaction descriptions, merchant raw names, or customer/transaction/external-transaction UUIDs logged merely for convenience. The access log carries only method, status, duration, the safe route template, `correlationId` via MDC, and standard ECS metadata. Regression-tested directly: a real request carrying a genuine `Authorization: Bearer <token>` header produces a captured log line that does not contain the token value anywhere.

**Authenticated principal identifier (SAD 37.1) is a deliberate, deferred gap in this branch.** Empirically verified against a real, successfully authenticated request (a genuine JWT, past `@PreAuthorize`, reaching the controller): by the time `CorrelationIdFilter` regains control after `filterChain.doFilter(...)` returns, both `request.getUserPrincipal()` **and** `SecurityContextHolder.getContext().getAuthentication()` are already `null` — Spring Security's own filter clears the security context in its own `finally` block before control unwinds back to this outermost filter. No filter-ordering change, no `SecurityContext` dependency in `config`, no capture-into-a-request-attribute workaround, and no re-parsing of the JWT was introduced merely to obtain `user.id` — that would mean redesigning security around a logging concern. Deferred pending a future, smaller-scoped change (most likely a controller-level log statement, where `Principal` is already legitimately available today via `java.security.Principal`, the same way `TransactionController` already obtains it).

**SQL/bind logging remains disabled**, unchanged by this branch — `spring.jpa.show-sql`, Hibernate SQL `DEBUG`, and Hibernate bind-parameter `TRACE` were never configured anywhere in this project, confirmed directly rather than assumed, and nothing here changes that.

**Audit vs. logging (SAD 37.5)** — unchanged distinction: `AuditEvent` remains the durable, compliance-grade record (`audit.*` is completely untouched by this branch); application logs remain purely operational diagnostics. No `AuditEvent` is ever mirrored into a log line, and no log statement's success/failure is part of any business outcome.

**stdout only.** No file appender, no `logging.file.name`/`.path`, no rotation policy, no async appender. Console output is this application's logging boundary — log shipping, aggregation, and retention are external, deployment-platform concerns (SAD 41.2's "Log Platform"), not implemented here. No OpenTelemetry, trace/span IDs, ELK/OpenSearch/Splunk/CloudWatch/Loki integration, or log-shipping agent of any kind is included.

**Testing:** `CorrelationIdFilterTests` covers the access-log event shape, safe-route inclusion/omission, existing MDC cleanup, and a new cross-request non-leakage test (a second, header-less request never inherits the first request's correlation id). `GlobalExceptionHandlerTests` covers exactly-one-`ERROR`-event-with-the-real-`Throwable` for the unexpected path, and zero log events for an expected business exception. `StructuredLoggingEndToEndTests` drives real HTTP requests through the real filter chain and captures the real `ILoggingEvent`, encoded through the exact same `CONSOLE` appender's encoder Boot itself configured (not a `System.out` swap, which Logback's `ConsoleAppender` captures a reference to before a test could ever redirect it) — proving genuine, valid ECS JSON, correct `correlationId`/method/status/duration/route, the generated-id-matches-response-header case, and token absence. No test snapshots an entire JSON log line; only specific, stable fields are asserted.

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

### Documentation Precedence

When two documentation sources disagree, the conflict is resolved using this precedence order, highest first:

1. Accepted ADRs (the register in SAD 49)
2. The current approved SAD files — those whose filenames contain `v_2` (`Solution_Architecture_Document(SAD)_v_2_Part_*.md`); the `v_2` filename is what makes a part authoritative. `Solution_Architecture_Document(SAD)_v_1.md` is superseded and retained only for history — it is never renamed or edited to match a later decision.
3. The Technical Design Specification (TDS)
4. This README

Field-shape conflicts between SAD and TDS entity/DDL definitions have recurred for both `customer` and `merchant` (different field names, TDS omitting `version`, different `VARCHAR` lengths). The established resolution is to follow SAD (it outranks TDS, and its inclusion of `version` is also backed by Accepted ADR-014), reporting the specific conflict rather than silently picking a side. A real SAD/TDS error-code naming conflict was found and resolved the same way in `feature/security`: SAD 39.4's error codes are used exactly (`TRANSACTION_DUPLICATE`, `SOURCE_NOT_FOUND`, etc.), never TDS 40's different `TRX-NNN`/`SEC-NNN` scheme. The create-transaction response follows SAD 35.1's shape (including nested `merchant`/`category` objects) over TDS 28's narrower documented shape, for the same reason.

When sources conflict: the conflict is reported rather than silently resolved by choosing one source; the affected files and sections are identified; a resolution is proposed; and the affected area is not implemented against until the conflict is recorded as resolved — normally via a new accepted ADR added to the register, not by editing an existing accepted ADR's rationale in place (mark it superseded instead).

### Documentation Rules

Whenever a major architectural decision changes (module boundaries, aggregate ownership, API contract, security model, database schema), this README, the relevant SAD part(s), the TDS, and the ADR register (SAD 49 — a new `ADR-0XX` entry) are all updated in the same change. Documentation must never be allowed to drift behind the code; if a documentation update is out of scope for a given change, that is stated explicitly rather than skipped silently.

## Future Roadmap

**Near-term:** distributed tracing, a structured reason on `TransactionValidationException` (to reach TDS 40's specific `TRX-002`/`TRX-004`/`TRX-005` codes instead of the current generic `REQUEST_VALIDATION_FAILED`), a focused Testcontainers concurrent-duplicate-write test against `JpaTransactionRepositoryAdapter` (considered during `feature/transaction-bulk` and deliberately deferred, since bulk itself introduces no new concurrency), category create/update/deactivate/delete (deliberately excluded from `feature/category-admin` pending a project decision — see [Category & Rule Administration](#category--rule-administration)), rule physical deletion, admin audit events, audit-event get-by-id, `actor`/`eventType` audit indexes (deferred pending real usage evidence — see [Audit Event Query](#audit-event-query)), audit export/retention/SIEM integration.

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

Every branch in the original sequence is now complete, plus eight post-MVP branches: `feature/transaction-query`, adding transaction retrieval/search (`GET /api/v1/transactions/{id}`, `GET /api/v1/transactions` — FR-08, UC-03, UC-04, TDS 30-31); `feature/transaction-bulk`, adding bulk transaction creation with partial success (`POST /api/v1/transactions/bulk` — FR-02, UC-02, SAD 32.5/35.2/39.8, TDS 29); `feature/category-admin`, adding category/categorisation-rule administration; `feature/audit-query`, adding audit-event search (`GET /api/v1/audit-events` — SAD 31.2's "operational and compliance queries") on top of the already-secured API — the second branch in a row whose scope was not fully derivable from SAD/TDS and required explicit project decisions instead (see [Category & Rule Administration](#category--rule-administration) and [Audit Event Query](#audit-event-query)); `feature/openapi-documentation`, adding generated OpenAPI/Swagger documentation for all 11 endpoints, gated by an empirical Springdoc/Spring-Boot-4.1 compatibility check before any annotation work began (see [OpenAPI / Swagger Documentation](#openapi--swagger-documentation)); `feature/observability`, adding Micrometer/Prometheus metrics scoped deliberately to metrics only, deferring the other three SAD 38.1 pillars (logs, traces, health/readiness) to separate branches (see [Observability](#observability)); `feature/health-readiness`, adding the liveness/readiness pillar deferred by `feature/observability` — Spring Boot 4.1's built-in probe groups, `db` added to the readiness group only, no custom health code, and an empirical three-state (available/interrupted/restored) Toxiproxy-based proof of the outage/recovery contract (see [Health & Readiness](#health--readiness)); and `feature/structured-logging`, closing the logs pillar `feature/observability` deferred — Spring Boot 4.1's native ECS structured-logging formatter, zero third-party dependency, with a safe route-template access log and single-point unexpected-exception logging (see [Structured Logging](#structured-logging)).

`feature/project-structure` established only the package skeleton, Spring Modulith module boundaries, architecture verification tests, and shared configuration structure — no business logic. `categorisation` and `audit` were built before `transaction` because the transaction ingestion workflow depends on both (assigning a category and recording an audit event are part of processing a transaction, not features bolted on afterwards). `aggregation` came after `transaction` because it only reads transaction data that must already exist. `api` wired already-completed use cases to HTTP without introducing new business behaviour, behind a temporary permit-all posture. `feature/security` replaced that posture with the real, documented JWT/RBAC model last, since it needed real HTTP endpoints to secure. `feature/transaction-query` came after all ten, as the first post-MVP gap-analysis-driven branch, adding two new `TRANSACTION_READ`-protected read endpoints and the batch (not per-row) cross-module enrichment lookups they need. `feature/transaction-bulk` followed, adding the documented bulk-create capability by calling the existing single-create use case once per item — see [Bulk Transaction Creation](#bulk-transaction-creation) for its partial-success/duplicate/error semantics. Before implementing a feature:

1. Read the relevant sections of `documentation/` for that module.
2. Confirm the module's package structure, ports, and dependency rules.
3. Implement with tests (unit, repository/integration, controller, and module-boundary tests as applicable).
4. Update `documentation/` if the implementation changes an architectural decision.

The engineering standards and workflow below apply equally to AI-assisted and human contributions.

## Engineering Standards & Coding Conventions

**Before writing code:** understand the requirement fully against `documentation/` (not guessed), explain the proposed solution in plain terms, identify risks/trade-offs/ambiguity, and produce an implementation plan (files touched, module(s) affected, tests to add). For anything touching more than one module, changing a public port/interface, changing the database schema, or changing a security rule, the plan is agreed before writing code — a fast wrong answer is worse than a slightly slower correct one on a financial system.

**Standards enforced, in order of how often they get sacrificed for expediency:** SOLID; Domain-Driven Design; Clean Architecture; Clean Code; DRY (but not at the cost of coupling unrelated modules together); KISS; YAGNI (no building for a future requirement that isn't documented in the SAD/TDS); composition over inheritance; dependency inversion (depend on ports/interfaces, not concrete infrastructure types); immutability where appropriate (value objects, DTOs, processed transactions); meaningful naming (no abbreviations outside the domain glossary, SAD 53); small methods/classes, single responsibility; constructor injection only — **no field injection, ever**; interfaces at module boundaries (concrete classes are fine as private implementation detail); avoided premature optimisation, but never an ignored obvious N+1 or unbounded query.

**Coding rules:**
- Java 21 and Spring Boot 4 idioms throughout.
- Records for value objects/immutable DTOs; plain classes for JPA entities and mutable aggregates. An aggregate with no documented lifecycle (e.g. `Merchant`) stays a plain class with no mutation methods — no invented status/update capability just because a sibling aggregate has one.
- Lombok is not currently a dependency; if introduced, only where it removes genuine boilerplate, and never `@Data` on a JPA entity (breaks lazy loading/identity semantics, can leak associations into logs).
- `UUID` for every entity identifier; identity value objects generate their own values via a static factory (e.g. `CustomerId.generate()`), called by the application layer and handed to the aggregate's factory method — never JPA `@GeneratedValue`, which would leave the aggregate with a nullable id until after the first insert.
- `Instant` for every timestamp, persisted as `TIMESTAMPTZ`, treated internally as UTC.
- `BigDecimal` for all monetary amounts — floating-point types are prohibited for money, no exceptions. A monetary value object normalises to its own currency's real precision (e.g. `Money` normalises to scale 2 for ZAR, `RoundingMode.UNNECESSARY`), not to whatever scale the database column happens to use.
- `@Version` on the JPA entity for mutable aggregates, following the SAD's schema even for aggregates with no current mutation, wherever the SAD explicitly lists the field.
- "At most one row with a flag set" is a database-level invariant (a partial unique index — e.g. `transaction_categories (is_fallback) WHERE is_fallback = TRUE`), never an application-level check-then-insert, which would race under concurrent writes.
- Application services (`@Service`) are Spring-managed from the branch that introduces them, even before a real caller exists.
- Errors follow RFC 9457 (`ProblemDetail`), never a bespoke error envelope.
- Bean Validation at the API boundary plus explicit invariant checks in domain/application — API validation alone is never trusted as sufficient.
- JPA entities are never exposed through REST — always mapped to/from purpose-specific DTOs.
- Any dynamically-provided sort field or query parameter is whitelisted before it reaches persistence — never a client-supplied property name passed straight into a query.
- `@PreAuthorize` checks fine-grained authorities, never role names. Roles are collections of authorities assigned to a client (ADR-016) — every controller method needing one carries its own `@PreAuthorize`, the single source of authorization truth.
- Cross-module contracts stay security-neutral, the same primitives-only rule as domain-type leaks (see [Module Boundaries & Dependency Rules](#module-boundaries--dependency-rules)): `CreateTransactionCommand.actor()` is a plain `String` (the JWT `sub` claim), never a Spring Security or JWT type.

**Testing, in order of value for this codebase** (see [Testing](#testing) for what's actually implemented today):
1. Unit tests for domain and application logic (JUnit 5 + Mockito), no Spring context.
2. Repository tests against a real PostgreSQL via Testcontainers — never mock the database for persistence-layer tests.
3. Integration tests exercising a use case end-to-end within the Spring context.
4. Controller tests for request validation, status codes, and error-response shape.
5. Architecture tests — Spring Modulith's `ApplicationModules.verify()` (`ModularityTests`) plus the hand-rolled `FrameworkIndependenceAssertions`-based checks (`<Module>ArchitectureTests`), making module-boundary and layering violations a build failure, not a review comment.

Every new feature needs, at minimum: the happy path, each documented validation failure, and each documented error-code scenario for that endpoint/use case. A dedicated test purely to exercise a trivial getter is not written.

## Contribution Guidelines

- Branch per bounded context/feature (`feature/<module>` or `fix/<short-description>`).
- Run `./mvnw verify` locally before opening a pull request.
- New behaviour requires tests; keep coverage aligned with the testing strategy in [SAD 45](documentation/Solution_Architecture_Document%28SAD%29_v_2_Part_6A_Operations.md).
- Respect module boundaries and dependency rules — do not add controller-to-repository or cross-module repository access.
- Never expose JPA entities through the REST API; always map to/from DTOs.
- If a change affects an architectural decision, update the SAD/TDS/ADRs in the same pull request (see [Documentation Rules](#documentation-rules)).
- Keep pull requests scoped to a single module or concern where practical.

## License

No license has been declared for this project yet. Add a `LICENSE` file before public release or external distribution.
