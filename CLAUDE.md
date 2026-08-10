# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## Project Overview

The **Transaction Aggregation API** is a Spring Boot 4.1.0 REST API (Java 21, Maven) that consolidates financial transactions from multiple upstream sources into a single, standardised domain model. It ingests transactions (single and bulk), validates and deduplicates them, assigns categories through configurable rules, persists them in PostgreSQL, and exposes secure REST APIs for retrieval and financial aggregation (customer, category, merchant, and monthly summaries).

**Business purpose:** replace fragmented, per-provider transaction processing with one centralised platform, so every downstream consumer integrates with a single consistent API instead of duplicating validation, categorisation, and aggregation logic per source.

**Architecture:** a **Modular Monolith** built on **Domain-Driven Design** and **Clean Architecture**. It is one deployable Spring Boot application with strict internal module boundaries: each module owns its own data and exposes only explicit interfaces to the rest of the system. This gives strong transactional consistency and simple deployment today, with a deliberate, documented path to extracting modules into independent services later if scaling or ownership needs justify it. Do not introduce microservice-style infrastructure (service discovery, message brokers, API gateways) speculatively; that evolution happens only when the readiness indicators in the SAD are met.

**Goals:** correctness of financial data above all else (immutable transactions, no duplicate processing, exact decimal money), followed by maintainability (clean module boundaries that are actually enforced, not just documented), then observability and security as first-class, non-optional concerns.

This repository is in an early implementation phase. The SAD, accepted ADRs, and TDS form the current approved design baseline in `documentation/`; treat it as the source of truth, not as inspiration to be reinterpreted. Documented conflicts must be resolved (with an ADR, if the resolution is itself an architectural decision) before implementing the affected area. See [Documentation Precedence](#documentation-precedence) for how to resolve a disagreement between sources.

## Technology Stack

| Technology | Version | Notes |
|---|---|---|
| Java | 21 | Language baseline (`java.version` in `pom.xml`) |
| Spring Boot | 4.1.0 | Parent POM |
| Spring Modulith | 2.1.0 | `core`, `insight`, `jpa`, `runtime` starters; module boundary enforcement |
| Spring Web MVC | (Boot-managed) | REST layer |
| Spring Data JPA / Hibernate | (Boot-managed) | Persistence |
| PostgreSQL | `postgres:latest` locally | Pin an explicit version before any shared/prod environment |
| Flyway | `flyway-database-postgresql` | Schema migrations; `V1` (customers), `V2` (merchants), `V3`/`V4` (categorisation tables), `V5`/`V6` (categorisation seed data), `V7` (audit events), `V8`/`V9` (transaction tables), `V10` (transaction source seed data), `V11` (transaction search composite indexes, `feature/transaction-query`) exist so far |
| Spring Security + OAuth2 Resource Server | (Boot-managed) | JWT bearer authentication, `@PreAuthorize`-enforced authorities (`security.SecurityConfig`, since `feature/security`) |
| Spring Validation | (Boot-managed) | Jakarta Bean Validation |
| Spring Boot Actuator + Micrometer (Prometheus) | (Boot-managed) | Health, metrics |
| Maven | wrapped (`mvnw`, Maven 3.9.16) | Do not require a system-installed Maven |
| Docker / Docker Compose | n/a | Local PostgreSQL via `compose.yaml`; auto-managed by `spring-boot-docker-compose` |
| JUnit 5, Mockito, Testcontainers (PostgreSQL), Spring Boot Test, Spring Modulith Test | (Boot/Modulith-managed) | Test stack |

**Not yet dependencies. Do not assume they exist without adding them first:**
- `springdoc-openapi` (OpenAPI/Swagger UI is specified in the SAD but not yet in `pom.xml`).
- Lombok (not currently used; if introduced, see [Coding Rules](#coding-rules); never on JPA entities).
- ArchUnit as a project dependency: not declared directly, though `com.tngtech.archunit:archunit` is present transitively (runtime scope, pulled in by `spring-modulith-runtime`, which uses it internally). Do not write test code against that transitive copy; it's an implementation detail of Modulith, not a stable API for this project to depend on. Module-boundary enforcement relies entirely on Spring Modulith's own `ApplicationModules.of(...).verify()`, which covers cross-module boundaries but not intra-module layering (whether a class references Spring/Jakarta Persistence types, or a module's `application` package reaches into its own `persistence` package). That gap is covered by a small hand-rolled reflection helper (`FrameworkIndependenceAssertions`, under the `architecture` test package), reused across `SharedFrameworkIndependenceTests`, `CustomerArchitectureTests`, `MerchantArchitectureTests`, `CategorisationArchitectureTests`, `AuditArchitectureTests`, and `TransactionArchitectureTests`, each applying the same two checks (domain framework-independence, application not referencing persistence) to that module's own small, known class list. The "first real domain model" trigger previously noted here has now happened five times (`customer.domain`, `merchant.domain`, `categorisation.domain`, `audit.domain`, `transaction.domain`) and was assessed directly each time; reflection remained proportionate at this scale, so ArchUnit was not added. Revisit if the hand-rolled approach stops scaling as more modules add their own domain/application layers, or if a rule comes up that reflection can't express cleanly (e.g. anything needing real call-graph or package-cycle analysis).
- Jackson: `tools.jackson.databind.ObjectMapper`, not `com.fasterxml.jackson.databind.ObjectMapper` - Spring Boot 4.1.0 ships Jackson 3.x, which renamed the core databind package from `com.fasterxml.jackson.*` to `tools.jackson.*` (confirmed by inspecting the actual `jackson-databind` jar on the classpath before relying on it, not assumed from Jackson 2.x muscle memory). `writeValueAsString(...)` is unchecked in Jackson 3 (no `JsonProcessingException` to catch), unlike Jackson 2. `transaction.application.CreateTransactionService` is the first consumer, injecting the Boot-autoconfigured `ObjectMapper` bean to build `RecordAuditEventCommand.eventData` JSON safely rather than hand-building JSON strings.

## Commands

On Windows use `mvnw.cmd`; on Unix use `./mvnw`.

```bash
./mvnw compile             # compile sources
./mvnw test                 # run tests (Docker must be running for Testcontainers)
./mvnw verify               # full verification lifecycle (tests + quality gates)
./mvnw package               # build the executable jar
./mvnw spring-boot:run      # run the application locally
./mvnw clean                # remove build output

docker compose up            # start local PostgreSQL (compose.yaml), optional, see below
docker compose down           # stop and remove it
```

`spring-boot-docker-compose` is on the classpath as an optional runtime dependency, so running the application (`./mvnw spring-boot:run`) starts and stops `compose.yaml`'s PostgreSQL container automatically. This is a separate lifecycle from testing: `./mvnw test` does not use `compose.yaml` at all. `TestcontainersConfiguration` (`src/test/java/.../TestcontainersConfiguration.java`) provisions its own PostgreSQL container via Testcontainers for the test run, independent of whatever `compose.yaml` is doing. Docker must be running for both scenarios, but they are two distinct containers. Run `docker compose up` manually only when PostgreSQL needs to stay available independently of the running application (e.g. to inspect data with `psql`); it is not required for `./mvnw test`.

## Architecture

### Style

Modular Monolith + Domain-Driven Design + Clean Architecture, layered per module:

```
Presentation (api)  →  Application (use cases)  →  Domain (entities, value objects, invariants)  →  Infrastructure (JPA adapters, security, config)
```

The **domain layer must never depend on Spring, Jakarta EE, or any persistence/framework type.** It expresses business rules in plain Java (records for value objects, plain classes for entities). Framework annotations belong in the infrastructure and presentation layers only.

### Package Responsibilities

Base package: `za.co.tinyiko.transactionaggregation`. The 10 top-level module packages listed below already exist (`feature/project-structure`) as an empty skeleton: each has a `package-info.java` documenting its responsibility, and `shared` carries an explicit Spring Modulith `@ApplicationModule` declaration (see [Module Boundaries and Dependency Rules](#module-boundaries-and-dependency-rules)). The internal per-module layering shown below (`domain`, `application`, `port`, etc.) is the target structure from the Technical Design Specification; it is created incrementally, only as each module's feature branch adds real code to it. Every module is now fully populated, including `api` and `security` (shown below) - see [Implementation Rules](#implementation-rules) for the full delivery history.

```
za.co.tinyiko.transactionaggregation
├── api                     # shared presentation layer: controllers, request/response DTOs, exception advice
│   ├── controller           TransactionController (create/get/search), AggregationController (package-private, thin)
│   ├── dto
│   │   ├── request           CreateTransactionRequest
│   │   └── response          TransactionResponse, TransactionSearchResponse (+ nested PageInfo),
│   │                          TransactionSearchItemResponse, CustomerSummaryResponse, CategorySummaryResponse,
│   │                          MerchantSummaryResponse, MonthlySummaryResponse
│   ├── mapper                TransactionApiMapper (toResponse overloaded for TransactionCreatedResult and
│   │                          TransactionDetails, plus toSearchResponse), AggregationApiMapper (pure structural
│   │                          mapping, static)
│   └── advice                GlobalExceptionHandler (RFC 9457 ProblemDetail; error codes match SAD 39.4
│                               exactly, not TDS 40's TRX-NNN/SEC-NNN scheme - see Documentation Precedence;
│                               TRANSACTION_NOT_FOUND added in feature/transaction-query)
├── transaction              # owns transactions, transaction_sources
│   ├── domain               Transaction, TransactionId, TransactionSource, TransactionSourceId, Money,
│   │                          TransactionDirection (local to this module, see below), TransactionStatus, SourceStatus
│   ├── application          CreateTransactionUseCase, CreateTransactionCommand, CreateTransactionService,
│   │                          TransactionValidationException, TransactionSourceNotFoundException,
│   │                          CustomerNotFoundException (this module's own), DuplicateTransactionException,
│   │                          TransactionQueryPort, CustomerTransactionTotals, CategoryTransactionTotal,
│   │                          MerchantTransactionTotal, MonthlyTransactionTotal, TransactionQueryService,
│   │                          GetTransactionUseCase, TransactionDetails, TransactionNotFoundException,
│   │                          GetTransactionService, SearchTransactionsUseCase, TransactionSearchCriteria,
│   │                          TransactionSearchResultItem, PagedResult<T>, SearchTransactionsService
│   │                          (read-side additions, feature/transaction-query: FR-08/UC-03/UC-04/TDS 30-31 -
│   │                          TransactionDetails and TransactionCreatedResult stay deliberately distinct
│   │                          records, see TransactionDetails' own Javadoc)
│   │                          (public API exposed via a package-level @NamedInterface, first added when
│   │                          aggregation became transaction's first real cross-module reader)
│   ├── port                 TransactionRepositoryPort (save, findBySourceIdAndExternalTransactionId),
│   │                          TransactionSourceRepositoryPort (findByCode only), TransactionSummaryRepositoryPort
│   │                          (customerTotals/categoryTotals/merchantTotals/monthlyTotals - own internal row
│   │                          projections, never the application-layer result records; see below),
│   │                          TransactionSearchRepositoryPort (findDetailById, search - own internal row/query
│   │                          types TransactionDetailRow, TransactionSearchRow, TransactionSearchQuery,
│   │                          TransactionSearchPage, same "port owns its own shapes" rule as the summary port)
│   ├── persistence           TransactionEntity, TransactionSourceEntity, SpringDataTransactionRepository,
│   │                          SpringDataTransactionSourceRepository, SpringDataTransactionSummaryRepository
│   │                          (JPQL + one native aggregate query), SpringDataTransactionSearchRepository
│   │                          (extends JpaSpecificationExecutor; one JPQL constructor-expression query joining
│   │                          transaction_sources for findDetailById - see below), TransactionSpecifications
│   │                          (static Specification factory methods, null when a filter is absent),
│   │                          JpaTransactionRepositoryAdapter, JpaTransactionSourceRepositoryAdapter,
│   │                          JpaTransactionSummaryRepositoryAdapter, JpaTransactionSearchRepositoryAdapter
│   └── mapper                TransactionMapper (toDomain + toEntity, not applyTo), TransactionSourceMapper (toDomain only)
├── categorisation            # owns transaction_categories, categorisation_rules
│   ├── domain               TransactionCategory, TransactionCategoryId, CategorisationRule, CategorisationRuleId,
│   │                          Direction (local to this module, see below), MatchField, MatchOperator, CategorisationRuleEngine
│   ├── application          CategoriseTransactionUseCase, CategorisationInput (direction is a String, not Direction),
│   │                          CategorisationDecision (categoryId/matchedRuleId are UUID, not the domain id types), CategorisationService,
│   │                          GetCategoryUseCase (get, findIdByCode, getByIds - the latter two added in
│   │                          feature/transaction-query for categoryCode search-filter resolution and batch
│   │                          search-result enrichment), CategoryView (now carries categoryId, not just
│   │                          code/name), GetCategoryService
│   │                          (public API exposed via a package-level @NamedInterface, not per-class; see below)
│   ├── port                 CategoryRepositoryPort (findFallback, findByCode, findByIds - the latter two added
│   │                          in feature/transaction-query), CategorisationRuleRepositoryPort (findAllActive only)
│   ├── persistence           TransactionCategoryEntity, CategorisationRuleEntity, SpringDataTransactionCategoryRepository,
│   │                          SpringDataCategorisationRuleRepository, JpaCategoryRepositoryAdapter, JpaCategorisationRuleRepositoryAdapter
│   └── mapper                TransactionCategoryMapper, CategorisationRuleMapper (toDomain only, no applyTo; see below)
├── aggregation              # owns no tables; reads via transaction.application.TransactionQueryPort
│   ├── domain               DateRange (from/to LocalDate, both inclusive, max 24-month span - TDS 20)
│   └── application          GetCustomerSummaryUseCase, GetCategorySummaryUseCase, GetMerchantSummaryUseCase,
│                              GetMonthlySummaryUseCase (TDS 6's own four documented interfaces, not one
│                              consolidated use case), CustomerSummaryView, CategorySummaryView,
│                              MerchantSummaryView, MonthlySummaryView, GetCustomerSummaryService,
│                              GetCategorySummaryService, GetMerchantSummaryService, GetMonthlySummaryService,
│                              CustomerNotFoundException (this module's own)
│                              (no @NamedInterface yet - no caller exists until a future feature/api branch;
│                              no port/persistence/mapper packages - see below)
├── customer                 # owns customers
│   ├── domain              Customer, CustomerId, CustomerStatus
│   ├── application         CustomerLookupPort, CustomerExistsPort (takes a raw UUID, not CustomerId; see below),
│   │                        RegisterCustomerUseCase, RegisterCustomerCommand, CustomerLookupService,
│   │                        CustomerRegistrationService, CustomerNotFoundException, DuplicateCustomerReferenceException
│   │                        (public API exposed via a package-level @NamedInterface, same pattern as categorisation)
│   ├── port                 CustomerRepositoryPort
│   ├── persistence           CustomerEntity, SpringDataCustomerRepository, JpaCustomerRepositoryAdapter
│   └── mapper                CustomerMapper
├── merchant                  # owns merchants
│   ├── domain              Merchant (immutable, no status), MerchantId, MerchantNormaliser
│   ├── application          MerchantResolutionPort (returns MerchantResolutionResult, not Merchant; see below),
│   │                        MerchantResolutionResult, MerchantResolutionService, DuplicateMerchantException,
│   │                        GetMerchantsUseCase (get, findByIds), MerchantView, GetMerchantsService
│   │                        (GetMerchantsUseCase added in feature/transaction-query, transaction becoming
│   │                        merchant's second inbound-port caller on the read side; MerchantView kept
│   │                        deliberately distinct from MerchantResolutionResult despite an identical shape -
│   │                        find-or-create vs. pure read, see MerchantView's own Javadoc)
│   │                        (public API exposed via a package-level @NamedInterface, same pattern as categorisation)
│   ├── port                 MerchantRepositoryPort (save, findByNormalisedName, findByIds - the last added in
│   │                        feature/transaction-query for GetMerchantsUseCase.findByIds' batch lookup)
│   ├── persistence           MerchantEntity, SpringDataMerchantRepository, JpaMerchantRepositoryAdapter
│   └── mapper                MerchantMapper
├── audit                     # owns audit_events (append-only)
│   ├── domain               AuditEvent (immutable, append-only, no @Version), AuditEventId
│   ├── application          RecordAuditEventUseCase (returns void, not AuditEvent; see below), RecordAuditEventCommand,
│   │                          AuditService (always runs in its own REQUIRES_NEW transaction; see below)
│   │                          (public API exposed via a package-level @NamedInterface, same pattern as categorisation)
│   ├── port                 AuditRepositoryPort (save only; nothing reads an audit event back yet)
│   ├── persistence           AuditEventEntity (eventData mapped to JSONB via @JdbcTypeCode(SqlTypes.JSON)),
│   │                          SpringDataAuditRepository, JpaAuditRepositoryAdapter
│   └── mapper                AuditMapper (toDomain + toEntity, not applyTo; see below)
├── security                  # JWT validation, role/authority extraction, access-denied handling
│   ├── SecurityConfig         The only public type: SecurityFilterChain, @EnableMethodSecurity, the
│   │                          production issuer-based JwtDecoder (@Profile("!local")) and the local
│   │                          symmetric-key one (@Profile("local"))
│   ├── RoleClaimAuthoritiesConverter   ADR-016's role->authority mapping (package-private, no Spring
│   │                          bean - constructed directly by SecurityConfig, same as its siblings below)
│   ├── ProblemDetailAuthenticationEntryPoint, ProblemDetailAccessDeniedHandler   RFC 9457 401/403
│   │                          responses matching api.advice.GlobalExceptionHandler's shape
│   └── ProblemDetailSupport   shared response-writing helper for the two above (package-private)
├── config                     # cross-cutting Spring configuration (Jackson, JPA, clock, correlation ID filter, OpenAPI)
│   ├── ClockConfig           # the one Clock bean, added when customer's first @Service needed it
│   └── CorrelationIdFilter   # resolves/generates X-Correlation-ID, MDC, request attribute (feature/api);
│                               @Order(HIGHEST_PRECEDENCE) so it runs before security.SecurityConfig's
│                               filter chain - verified empirically that correlation IDs and MDC are
│                               populated for 401/403 responses too, not just successful ones
└── shared                     # reusable technical building blocks only
    ├── event                   # DomainEventEnvelope<T>: the common event wire format (TDS 45)
    └── logging                 # CorrelationId: request-tracing value object (SAD 34.10, 37)
```

`shared` deliberately does not (yet) have an exception hierarchy or a `util` package; see [Module Boundaries and Dependency Rules](#module-boundaries-and-dependency-rules) for why.

Controllers live in the shared top-level `api` package (thin, no business logic) and call into each module's `application` use-case interfaces. The `api` layer must never depend on a module's `domain`, `port`, or `persistence` types directly, only its `application`-layer interfaces.

**A module's shape follows its own documented capabilities, not the previous module's shape.** `customer` has three inbound ports (lookup, exists, register) and two exceptions because three separate capabilities and two distinct failure scenarios are documented for it. `merchant` has one inbound port (`MerchantResolutionPort`, a fused find-or-create operation) and one exception (`DuplicateMerchantException`, used internally for recovery, not a caller-facing "not found"/"conflict" pair) because that's what its own documentation calls for. `categorisation` has a single inbound port (`CategoriseTransactionUseCase`) and no application-level exception at all: the only "nothing matched" case is handled internally by falling back to the seeded fallback category, not by throwing to a caller (see `CategorisationService`). `audit` likewise has a single inbound port (`RecordAuditEventUseCase`) and no application-level exception at all: there is no documented failure scenario for recording an audit event, and its outbound port is write-only (`AuditRepositoryPort.save` only) since nothing reads an audit event back yet despite the `AUDIT_READ` authority being reserved for a future query capability. `transaction` is the first module with a genuinely multi-way dependency list (`customer`, `merchant`, `categorisation`, `audit`, `shared`) and a single inbound port (`CreateTransactionUseCase`) with four distinct application-level exceptions, one per documented failure scenario (validation, source not found/inactive, customer not found, duplicate) - because TDS's error catalogue documents four genuinely distinct failure codes for transaction creation, not because "customer had two so this should have some too." `aggregation` has four inbound ports (matching TDS 6's own four documented interfaces exactly, not consolidated into one) and only one exception (`CustomerNotFoundException`) despite four use cases, because all four share the identical single failure scenario (the requested customer doesn't exist) - an empty result set is not a failure scenario here, it's an ordinary all-zero/empty response (see [Persistence Ownership](#ports-and-adapters)). When starting a new module, work out what *that module* needs from the SAD/TDS before assuming it should look like the last one.

### Module Boundaries and Dependency Rules

- `shared` contains only reusable, business-rule-free abstractions. Every module may depend on it; it depends on nothing else in the codebase. It currently holds `DomainEventEnvelope<T>` (`shared.event`) and `CorrelationId` (`shared.logging`), both added in `feature/shared` because they're already needed by multiple already-specified future modules (TDS 45's event catalogue spans 2 modules; correlation IDs flow through every request, audit event, and error response). `audit` is the first real consumer of `CorrelationId` (`AuditEvent`/`RecordAuditEventCommand` both hold one) - confirming that forward-building call was justified rather than speculative; `transaction` is the second, generating one per `CreateTransactionUseCase.create` call to pass through to `RecordAuditEventUseCase` (a placeholder value, not a real propagated request correlation id - see [Deferred Work](#implementation-rules) below). A shared exception hierarchy, `shared.validation`, `shared.util`, `DomainEventPublisher`, and `ClockProvider`/`CurrentUserProvider` were deliberately **not** built: each would currently have zero consumers, so building them now would be preparing for modules that don't exist rather than solving a known problem. `java.time.Clock` is used directly instead of a custom `ClockProvider`, since it already provides everything a wrapper would, with none of the drawbacks. Add any of these deferred items only when the first real consumer needs it, not preemptively.
- **Sharing a concept across two modules is not, by itself, a reason to promote it into `shared`.** `categorisation.domain.Direction` (`CREDIT`/`DEBIT`) stayed local to `categorisation` even now that `transaction` exists and needs an equivalent concept (`transaction.domain.TransactionDirection`): the two are connected only by an explicit `.name()` string crossing the module boundary inside `CreateTransactionService`, not a shared type. This turned out to be the right call rather than a deferred one: `categorisation.application.CategorisationInput.direction` was changed to a plain `String` specifically so `transaction` never needs to reference `categorisation.domain.Direction` at all (see the `@NamedInterface` bullet below) - there was never a real need to unify the two enums, only a need to stop a domain type leaking across the boundary, which a contract change solved without touching `shared`.
- **Cross-module contracts return/accept primitives (`String`, `UUID`) or types from the target module's own `application` package, never a type from its `domain` package - even indirectly, as a field on an otherwise-exposed `application` type.** This was discovered, not designed upfront: `categorisation.application.CategorisationInput`/`CategorisationDecision` and `audit.application.RecordAuditEventUseCase` were originally shaped with `categorisation.domain.Direction`/`TransactionCategoryId`/`CategorisationRuleId` and `audit.domain.AuditEvent` respectively, which compiled fine and passed every test *until* `transaction` became the first real cross-module caller and needed to construct/consume those types - at which point they were just as inaccessible as if `customer.application`/`merchant.application` had never been given a `@NamedInterface` at all, since exposing a package doesn't expose the packages that its own exposed types happen to reference. Resolved by revising the contracts rather than by also exposing the `domain` packages: `CategorisationInput.direction` became a `String`, `CategorisationDecision`'s ids became `UUID`, `RecordAuditEventUseCase.record` became `void`, and `MerchantResolutionPort.resolve` returns a new `MerchantResolutionResult` (`UUID merchantId, String displayName`) instead of the domain `Merchant`. `CustomerExistsPort.exists` similarly takes a raw `UUID`, not `CustomerId`. Check this specifically the first time a module gains a real second caller, not just whether the package itself is exposed.
- Allowed module dependency directions: `transaction → categorisation`, `transaction → audit`, `transaction → merchant`, `transaction → customer`, `aggregation → transaction` (read-only), `aggregation → customer` (read-only). Do not introduce a dependency running the other way, and never a cycle. `categorisation` itself depends on nothing but `shared`: despite TDS 5 listing "Merchant module" under its dependencies, that's a business-level statement, not a code dependency, since `categorisation` receives merchant text as a plain `String` (`CategorisationInput.merchantText`), never calling into `merchant`'s ports or referencing its types. `aggregation`'s dependency list is deliberately narrow (`transaction`, `customer` only - no `merchant`, no `categorisation`) matching SAD's own dependency diagram and TDS 6's "Dependencies" section exactly: neither documents aggregation needing merchant or category names, so `CategorySummaryView`/`MerchantSummaryView` expose raw `categoryId`/`merchantId` only, leaving name resolution to a future `feature/api` layer that already has a legitimate reason to call those modules directly.
- **Within a single module, the dependency direction is `application → port → persistence`, never the reverse - this applies inside a module, not just across module boundaries.** A real mistake caught during review illustrates why this needs saying explicitly: `transaction.port.TransactionSummaryRepositoryPort` was first drafted to return the public `transaction.application` result records (`CustomerTransactionTotals` etc.) directly, which would have made `transaction.port` depend on `transaction.application` - backwards, and a real dependency cycle risk (`application` already depends on `port`). The fix was giving the port its own internal, primitive-based row types (`CustomerTotalsRow`, `CategoryTotalsRow`, `MerchantTotalsRow`, `MonthlyTotalsRow`, all in `transaction.port`), with `TransactionQueryService` (in `transaction.application`) mapping between the two field-for-field-identical-but-distinct shapes. `architecture.TransactionArchitectureTests#portDoesNotReferenceApplication` now guards against this regressing. Apply the same "port gets its own internal result types" pattern whenever a future outbound port's natural return shape would otherwise tempt reusing a public application-layer type.
- A module may **never** access another module's JPA repository or persistence package directly. Cross-module reads/writes go through the target module's `application` use-case interfaces or `port` query interfaces; asynchronous reactions go through domain events.
- Controllers never depend on repositories, only on `application` use-case interfaces.
- Persistence entities are never returned from a controller or crossed a module boundary; map to/from DTOs and domain types at the edges.
- The `aggregation` module owns no persistent tables and must not write transaction data; it composes summaries by reading through `transaction`'s query port.
- These rules should be enforced as executable tests using Spring Modulith (`ApplicationModules.of(TransactionAggregationApiApplication.class).verify()`, in `ModularityTests` under the `architecture` test package), not left as documentation-only conventions. Treat a failing module-verification test the same as a failing compile.
- Modules are recognised by Spring Modulith's default package-based convention alone: a bare `package-info.java` is enough, no annotation required. `verify()`'s built-in encapsulation of nested (non-root) packages already enforces "no direct cross-module persistence access" and "controllers cannot depend on repositories" with zero extra configuration. Add an explicit `@ApplicationModule` annotation only where a rule genuinely cannot be expressed any other way: `shared` carries one (`type = OPEN, allowedDependencies = {}`); `categorisation` carries one too (`allowedDependencies = {}`); `audit` carries one as well (`allowedDependencies = "shared"`); `transaction` carries one with a genuinely multi-way list (`allowedDependencies = {"customer :: application", "merchant :: application", "categorisation :: application", "audit :: application", "shared"}`); and `aggregation` now carries one too (`allowedDependencies = {"transaction :: application", "customer :: application"}` - no `merchant`, `categorisation`, `audit` or `shared`, matching its genuinely narrower documented dependency list exactly). See the qualified-syntax bullet immediately below for why these aren't bare module names. `customer` and `merchant` were both deliberately left without an annotation, for consistency: package-based detection already covers everything currently needed for them. Do not add annotations to other modules "for documentation"; the package-info Javadoc already documents intent, and an annotation should only appear when it changes verified behaviour.
- **`allowedDependencies` entries that target a specifically-named interface must use the qualified `"module :: interfaceName"` syntax, not a bare module name.** A bare module name only grants access to that module's *default* (unnamed) interface; it does not implicitly cover every `@NamedInterface` the target module happens to expose. `transaction`'s `allowedDependencies` was first written with plain `"customer"`, `"merchant"`, `"categorisation"`, `"audit"` (mirroring `audit`'s own `allowedDependencies = "shared"`), and `verify()` rejected every one of them: `customer.application`/`merchant.application`/`categorisation.application`/`audit.application` each expose their API via a `@NamedInterface` implicitly named after the package's own simple name (`"application"`), not the module's default interface. The fix was the qualified form shown above - `shared` alone stays a bare name, since it's `type = OPEN` and exempt from this distinction entirely (the same reason it doesn't need to be explicitly listed as a target the way `audit`'s own dependency on it does need explicit listing as a *source* module's declaration - two different rules, easy to conflate). This is the same "trust the failing test over a copied pattern" lesson `audit`'s own earlier `allowedDependencies = {}` mistake taught, one syntax detail deeper: read `verify()`'s exact error message rather than assuming the allow-list syntax from a sibling module's declaration that happens to look similar.
- **Cross-module reads that enrich a collection of rows (e.g. a paginated search result) use a batch lookup contract, not a per-row call into another module.** `transaction.application.SearchTransactionsService` (feature/transaction-query, TDS 30-31) resolves each search row's merchant/category display data by collecting the distinct non-null merchant/category ids across the whole page once, then calling `merchant.application.GetMerchantsUseCase.findByIds`/`categorisation.application.GetCategoryUseCase.getByIds` exactly once each (skipped entirely when the page needs no such enrichment) - both backed by a single `IN (...)` SQL query via Spring Data's inherited `findAllById`, never a loop of individual lookups at either the cross-module-call level or the underlying SQL level. Reach for this pattern whenever a future read path would otherwise call another module's port once per row.
- **An unresolvable filter code crossing a module boundary resolves to a guaranteed-non-matching sentinel, never to `null`.** `SearchTransactionsService.resolveSourceId`/`resolveCategoryId` (feature/transaction-query) substitute `UUID.randomUUID()` when a caller-supplied `sourceCode`/`categoryCode` doesn't resolve to a real id, rather than `null` - `null` would be indistinguishable from "no filter requested" once it reaches `transaction.persistence.TransactionSpecifications`, silently turning an unknown-code filter into "match everything" instead of the correct empty result set. Apply the same substitution whenever a future filter needs to cross from a caller-supplied code to an id-based persistence filter with no matching row.
- **Cross-module API exposure uses a package-level `@NamedInterface`, not per-class annotations.** Spring Modulith's default encapsulation only exposes a module's *root* package; nested packages like `categorisation.application` or `audit.application` are internal and unreachable from other modules by default, even for public types, confirmed empirically before this was relied on. `categorisation.application`'s `package-info.java` carries a single `@org.springframework.modulith.NamedInterface`, exposing every public type in that package as one deliberate API surface, rather than annotating each exported type individually; `audit.application`, `customer.application` and `merchant.application` all follow the identical pattern. `CategorisationService`, `AuditService`, `CustomerLookupService`/`CustomerRegistrationService` and `MerchantResolutionService` all stay unreachable regardless, since they're package-private. `categorisation` was the first module to actually need cross-module exposure; `audit`, `customer` and `merchant` are further applications of the same pattern, the latter two triggered specifically by `transaction` becoming their first real caller; `transaction.application` now carries one too, triggered specifically by `aggregation` becoming *its* first real caller (on the read side - `CreateTransactionUseCase` had no external caller before this). **Exposing the package is necessary but not sufficient** - see the "cross-module contracts return/accept primitives" bullet above for the follow-on gap this alone doesn't close. `aggregation.application` itself has no `@NamedInterface` yet: nothing outside `aggregation` calls into it in this branch (a future `feature/api` is its only documented future caller) - add one only once that branch actually needs it, not preemptively, the same discipline `transaction.application` itself was held to for two branches before this one.

### Aggregate Ownership

| Aggregate root | Module | Notes |
|---|---|---|
| `Customer` | customer | Owns customer identity/status lifecycle |
| `Merchant` | merchant | Normalised merchant identity; immutable after creation, no status |
| `Transaction` | transaction | Immutable once successfully processed; sole writer of `transactions` |
| `TransactionSource` | transaction | Reference data for ingestion sources |
| `TransactionCategory`, `CategorisationRule` | categorisation | Two aggregate roots in one module |
| `AuditEvent` | audit | Append-only; no update or delete operations, ever |

Each module is the only writer of its own tables (see the ownership matrix in SAD 31.2). Other modules read owned data only through the owning module's ports, never through direct table/repository access.

### Ports and Adapters

Within each module, `port` defines outbound interfaces the domain/application layer needs (e.g. `CustomerRepositoryPort`, `MerchantRepositoryPort`). `persistence` provides the adapters that implement those ports using Spring Data JPA (e.g. `JpaCustomerRepositoryAdapter` wrapping `SpringDataCustomerRepository`). Inbound ports are the `application` use-case interfaces that controllers, or other modules, call (e.g. `CustomerLookupPort`, `MerchantResolutionPort`). New integrations (a future message broker, an external HTTP client) get their own adapter implementing an existing or new port; the domain/application layer never depends on the adapter's concrete type.

**Read-only ports need no `applyTo` mapper method or entity setters justified by production code.** `CategoryRepositoryPort`/`CategorisationRuleRepositoryPort` only define reads (`findFallback`, `findAllActive`); nothing in `categorisation` writes a category or rule through application code; `transaction_categories`/`categorisation_rules` are populated exclusively by the `V5`/`V6` Flyway seed migrations. `TransactionCategoryMapper`/`CategorisationRuleMapper` therefore have only `toDomain`, no `applyTo`. `TransactionCategoryEntity`/`CategorisationRuleEntity` still carry full setters, but purely so their own mapper tests can construct fixtures without a database; their Javadoc says so explicitly, so a future reader doesn't mistake them for evidence of a write path that doesn't exist. Do not add a `save`-shaped port method just because a sibling module's port has one; add it only once a real use case needs to write that data.

**Append-only aggregates use a `toEntity` mapper method, not `applyTo`.** `applyTo(entity, domain)` exists specifically to mutate an *already-managed, possibly pre-existing* JPA entity in place, so Hibernate's dirty-checking and `@Version` increment stay correct on update. `AuditEvent` (SAD 27.7: append-only, never updated after creation) never has a pre-existing managed entity to look up and mutate - every `save` is a brand-new row - so `AuditMapper.toEntity(domain): AuditEventEntity` builds a fully-formed entity via its all-args constructor instead. `AuditEventEntity` correspondingly has no setters and no `@Version` (SAD's own `audit_events` DDL has no version column either). Reach for `toEntity` instead of `applyTo` whenever a future aggregate is genuinely create-only with no update path, rather than defaulting to the mutate-in-place pattern out of habit. This is a different case from the read-only ports above (`categorisation`'s ports have no write path at all, so their mappers have no `applyTo` *or* `toEntity`); `audit` does write, just never updates what it wrote.

**A module can legitimately have no `port`, `persistence`, or `mapper` package at all.** `aggregation` doesn't - not because it's incomplete, but because SAD 31.3 is explicit that it "does not initially own persistent business data" and the ownership matrix lists its owned tables as "None." Every number `aggregation` returns comes from `transaction.application.TransactionQueryPort`; there is nothing of its own to read or write, so there's no outbound port to define and no adapter to implement it. Do not add a persistence layer to a module just because every other module has one - only once that module genuinely owns data, per SAD's own ownership matrix, not by default.

**A module can, and here does, have more than one outbound port when the read and write concerns are genuinely separate.** `transaction.port.TransactionSummaryRepositoryPort` is a second, distinct outbound port alongside `TransactionRepositoryPort` - not an addition of four read methods to that existing port - because it serves a different capability (pre-aggregated reporting reads) with a different, purpose-built Spring Data interface (`SpringDataTransactionSummaryRepository`) and adapter (`JpaTransactionSummaryRepositoryAdapter`) behind it. Bolting read-aggregate methods onto an already-focused, already-tested write/duplicate-check port would blur its purpose; this mirrors `categorisation` already keeping `CategoryRepositoryPort`/`CategorisationRuleRepositoryPort` as two single-purpose ports rather than one combined one.

**Database-side aggregation (`SUM`/`COUNT`/`GROUP BY` in SQL), not loading rows into memory to sum in Java.** `JpaTransactionSummaryRepositoryAdapter` is the first place in this codebase computing aggregates, and does so entirely in SQL: JPQL `@Query` with constructor-expression projections straight into the port's own internal row types (see the "application → port → persistence" bullet above) for three of its four queries, and one native query for the fourth (see below). Loading every matching transaction into the JVM to sum in a loop doesn't scale and isn't necessary when PostgreSQL already does this efficiently - the standard tradeoff favouring DB-side aggregation whenever a summary/reporting query is the actual requirement, not a documented need to inspect individual rows.

**`COALESCE(SUM(...), 0)` is required, not defensive-only, wherever a query can legitimately return zero matching rows.** SQL's `SUM` over zero rows returns `NULL`, not zero - `SpringDataTransactionSummaryRepository#customerTotals` would throw a `NullPointerException` out of its JPQL constructor expression for a customer with no transactions in range without it. `GROUP BY` queries (`categoryTotals`, `merchantTotals`) don't strictly need it, since a group with zero rows never appears in a `GROUP BY` result set at all - included anyway for consistency and as defensive-only insurance there. Verified empirically against a real empty result set via Testcontainers, not assumed from SQL semantics alone.

**Month-grouping needs a native query, and needs the timezone made explicit - both discovered empirically, not assumed.** `monthlyTotals` uses `@Query(nativeQuery = true)`, not JPQL: JPQL constructor expressions require an exact Java-constructor type match, and there's no portable JPQL expression that truncates a timestamp to month-start and projects cleanly into `java.time.YearMonth` - the native query returns raw rows that `JpaTransactionSummaryRepositoryAdapter` converts to `YearMonth` in Java instead. More importantly: a first attempt using bare `date_trunc('month', occurred_at)` on the `timestamptz` column produced *wrong* results in testing - a transaction occurring at midnight UTC on the 1st of a month was bucketed into the *previous* month, because PostgreSQL's `date_trunc` truncates in the current session's timezone, not UTC, and the test session's timezone wasn't UTC. The fix is `date_trunc('month', occurred_at AT TIME ZONE 'UTC')`, converting to a UTC wall-clock timestamp before truncating, consistent with this codebase's "treat everything internally as UTC" rule - and confirming that rule needs active enforcement in raw SQL, not just assumed from storing everything in `TIMESTAMPTZ` columns. Re-verify this specifically (not just "does it compile") whenever a future query does its own date/time truncation or bucketing in SQL.

**Translating persistence exceptions:** when a persistence adapter's write can violate a database constraint the application layer needs to react to (e.g. a unique-constraint race under concurrent writes), the adapter catches the Spring/JDBC exception (`DataIntegrityViolationException`) and rethrows a domain/application exception from that module's `application` package (e.g. `merchant.persistence.JpaMerchantRepositoryAdapter` catching it and throwing `merchant.application.DuplicateMerchantException`). Application code must never need to import a Spring persistence exception type. Two details that matter: the adapter must use `saveAndFlush`, not `save`, or Spring Data JPA may defer the actual `INSERT`/constraint check past the method's return (to a later flush or transaction commit), letting the violation escape uncaught; and the application layer, if it can recover (e.g. by re-querying for whatever a concurrent request just created), should, rather than letting the exception reach a caller for a condition the caller has no reasonable way to handle. **Recovery is a per-case decision, not automatic just because the pattern matches**: `merchant.persistence.JpaMerchantRepositoryAdapter` recovers by re-querying and returning the concurrently-created row, because merchant resolution is a find-or-create operation with no meaningful difference between "found it first" and "someone else created it a moment earlier." `transaction.persistence.JpaTransactionRepositoryAdapter` deliberately does **not** recover the same way for the identical-looking race on `(transaction_source_id, external_transaction_id)`: BR-08 requires a duplicate transaction to be *rejected* (HTTP 409), not silently resolved to the existing row, so the adapter just translates and rethrows `DuplicateTransactionException` with no re-query.

### Transaction Boundaries

- One HTTP write request executes within one application-level transaction, owned by the `application`-layer use-case, not the controller and not the repository.
- Bulk ingestion processes each item in its own transaction boundary so a single invalid item never rolls back the rest of the batch (per-item partial success is a hard requirement; see SAD BR rules and the bulk API contract).
- Do not place long-running or external (network) calls inside a database transaction.
- **Audit writes always run in their own independent transaction (`@Transactional(propagation = Propagation.REQUIRES_NEW)` on `audit.application.AuditService.record`), never inside the caller's transaction.** An audit record must survive even when the operation it describes fails and rolls back its own transaction - `transaction.application.CreateTransactionService` records a `TRANSACTION_DUPLICATE_REJECTED`/`TRANSACTION_VALIDATION_FAILED`/etc. audit event and then throws, and that audit row must not disappear along with the rollback of the rejected attempt. `REQUIRES_NEW` suspends the caller's (possibly already rollback-marked) transaction and commits the audit write on a genuinely separate physical transaction/connection, which is why this works even after a `saveAndFlush` failure has marked the caller's transaction rollback-only. This is the standard justification for `REQUIRES_NEW` in audit logging generally, not a `transaction`-specific workaround - apply the same treatment to any future module recording something that must outlive the operation it's about.
- Optimistic locking (`@Version`) protects mutable aggregates from lost updates; immutable processed transactions don't need update-oriented locking after creation.

### Adding a New Module

1. Create a new top-level package under the base package with `domain`, `application`, `port`, `persistence` (and `mapper`/`event`/`rule` as needed).
2. Define the domain model first (entities, value objects, invariants) with no framework dependencies.
3. Define outbound ports as interfaces before writing the JPA adapter that implements them.
4. Add the module to the dependency diagram and ownership matrix in the SAD, and get the dependency direction agreed before writing code. This is not a mechanical step, it changes the architecture.
5. Add a Flyway migration for any new tables, following the `V{n}__description.sql` convention. Check `src/main/resources/db/migration` for the actual next number; don't assume it from the SAD's illustrative sequence.
6. Add or extend `ModularityTests` (under the `architecture` test package) to cover the new module's allowed dependencies, and a module-specific `<Module>ArchitectureTests` (reusing `FrameworkIndependenceAssertions`) for its domain/application layering.
7. Follow the [Implementation Rules](#implementation-rules); implement it as its own `feature/<module>` slice, independently compilable.

### Where New Things Belong

| Adding a... | Goes in |
|---|---|
| REST endpoint / controller | `api.controller` (shared top-level), thin, delegates to the module's `application` use case |
| Request/response DTO | `api.request` / `api.response` |
| JPA / domain entity | `<module>.domain` (plain domain model) + a corresponding JPA entity in `<module>.persistence`. These are two distinct types, never the same class |
| Repository | Interface in `<module>.port`; Spring Data interface + adapter implementation in `<module>.persistence` |
| DTO-level validation | Jakarta Bean Validation annotations on `api.request` types |
| Business-invariant validation | Inside `<module>.domain` (constructors/factory methods enforcing invariants) and `<module>.application` (use-case-level checks). Validation is layered, not just done once at the API boundary |
| Mapper | `<module>.mapper` (e.g. an API mapper between `api` DTOs and `application` types, and a persistence mapper between domain and JPA entities) |
| Domain event | The event type itself in `<module>.event`, wrapped in `shared.event`'s `DomainEventEnvelope<T>` for publication. There is no `DomainEventPublisher` port yet; it's deferred until the first module actually publishes something. That module's branch should introduce it in `shared.event` rather than inventing a module-local one |
| Inbound use-case port | `<module>.application`. One interface per *documented capability* for that specific module, even before a controller or another module calls it. This is not always three ports like `customer`, or one like `merchant`; match what the module's own SAD/TDS text actually describes |
| Business exception (not found, conflict, etc.) | `<module>.application`, extending `RuntimeException` directly. Only add one where a genuine failure scenario needs to be signalled to a caller (or recovered from internally, like `DuplicateMerchantException`); don't add a "not found"/"duplicate" pair reflexively just because the previous module had one. No shared exception base exists yet; one is only worth introducing once a *second* module needs the same shape, demonstrating the commonality is real rather than assumed. Never attach an HTTP status to it; that mapping belongs in the still-unbuilt `api.advice` |

## Development Principles

Claude must always behave as a **Senior Software Engineer, Software Architect, Backend Engineer, Code Reviewer, Security Engineer, and Performance Engineer** simultaneously, not just whichever hat is most convenient for the immediate request.

Before writing any code:

1. Understand the requirement completely. Re-read the relevant `documentation/` sections rather than guessing.
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
- DRY, but not at the cost of coupling unrelated modules together
- KISS
- YAGNI: do not build for a future requirement that isn't documented in the SAD/TDS
- Composition over inheritance
- Dependency inversion (depend on ports/interfaces, not concrete infrastructure types)
- Immutability where appropriate (value objects, DTOs, processed transactions)
- Meaningful naming: no abbreviations that aren't already established in the domain glossary (SAD 53)
- Small methods, small classes, single responsibility
- Constructor injection only: **no field injection, ever**
- Prefer interfaces at module boundaries; concrete classes are fine as private implementation detail
- Avoid premature optimisation, but do not ignore an obvious N+1 query or unbounded query

## Coding Rules

- Java 21 and Spring Boot 4 idioms throughout.
- Use records for value objects and immutable DTOs where they fit; use plain classes for JPA entities and mutable aggregates. An aggregate with no documented lifecycle (e.g. `Merchant`) can be a plain class with no mutation methods at all; don't invent a status or update capability that isn't documented just because a sibling aggregate (e.g. `Customer`) has one.
- Lombok, if introduced, is used only where it removes genuine boilerplate (e.g. builders on request DTOs). **Never `@Data` on a JPA entity** (it generates `equals`/`hashCode`/`toString` that break lazy loading, identity semantics, and can leak associations into logs).
- Use `UUID` for all entity identifiers.
- Use `Instant` for all timestamps; persist as `TIMESTAMPTZ`; treat everything internally as UTC.
- Use `BigDecimal` for all monetary amounts: floating-point types are prohibited for money, no exceptions.
- **A monetary value object normalises to its own currency's real precision, not to whatever scale the database column happens to use.** `transaction.domain.Money` normalises `amount` to scale 2 in its compact constructor (`amount.setScale(2, RoundingMode.UNNECESSARY)`), even though `transactions.amount` is `NUMERIC(19,4)` (SAD 27.2/29.3's literal DDL). Scale 2 is a genuine business fact (ZAR's ISO 4217 minor-unit precision, TDS 16: "scale must not exceed two decimal places"); scale 4 is an incidental fact about the column type SAD's DDL happens to specify, and tying a domain value object's validation to that would be exactly the persistence-driven design SAD 26.1 says to avoid. It also resolves a genuine correctness problem: PostgreSQL always returns `NUMERIC(19,4)` values padded to scale 4, so a `Money` that validated at scale 4 would fail its own check when reconstructed from data it had itself written, unless normalised back down first. `RoundingMode.UNNECESSARY` (not a rounding mode) means this single call both normalises (strips the database's zero-padding, which always succeeds since the removed digits are genuinely zero) and validates (rejects real sub-cent precision loss) at once. Apply the same reasoning to any future monetary or precision-sensitive value object: normalise to the value's own real-world precision, not to the column's.
- Use optimistic locking (`@Version`) on the JPA entity for mutable aggregates, and follow the SAD's schema even for aggregates with no current mutation (e.g. `merchants.version`) if the SAD explicitly lists the field; it costs nothing to keep the schema consistent with the documented design. The domain aggregate itself does not carry the version; it's a persistence concern. The mapper's job on save is to write the domain's field values onto the already-loaded, managed JPA entity (not construct a detached replacement), so Hibernate's own dirty-checking and version increment handle the locking correctly.
- **"At most one row with a flag set" is a database-level invariant, not an application-level check.** SAD 27.5 requires at most one `transaction_categories` row with `is_fallback = TRUE`. Rather than checking-then-inserting in application code (a race under concurrent writes) or a `CHECK` constraint (can't reference other rows), `V3__create_transaction_categories_table.sql` uses a partial unique index: `CREATE UNIQUE INDEX ... ON transaction_categories (is_fallback) WHERE is_fallback = TRUE`. PostgreSQL only enforces uniqueness among rows matching the `WHERE` clause, so any number of `FALSE` rows are unrestricted while a second `TRUE` row is rejected outright; verified empirically against a real `postgres:latest` container before relying on it. Reach for this pattern whenever a future single-row-among-many invariant comes up, rather than an application-level check.
- Identity value objects (e.g. `CustomerId`, `MerchantId`) generate their own values via a static factory (`CustomerId.generate()`), called by the application layer and passed into the aggregate's factory method; the aggregate is handed a fully-formed identity, it does not decide how identities are produced. Not JPA `@GeneratedValue` either, which would leave the aggregate with a nullable id until after the first insert.
- Application services (`@Service`) are Spring-managed from the branch that introduces them, even before a controller or another module calls them; Spring wiring is not deferred "until there's a real caller." If a service needs `Clock`, inject it; the bean lives in `config.ClockConfig`.
- See [Ports and Adapters](#ports-and-adapters) for how to translate a persistence-layer exception into a domain/application one instead of letting it leak.
- Errors follow RFC 9457: use Spring's `ProblemDetail`, never a bespoke error envelope.
- Validate all input: Bean Validation at the API boundary, explicit invariant checks in the domain/application layers. Never trust that API validation alone is sufficient.
- Never expose JPA entities through REST. Always map to/from purpose-specific DTOs.
- Whitelist any dynamically-provided sort field or query parameter before it reaches persistence; never pass client-supplied property names straight into a query.
- `@PreAuthorize` checks fine-grained authorities (`TRANSACTION_WRITE`, `CUSTOMER_READ`, etc.), never role names. Roles (`ROLE_API_CONSUMER`, `ROLE_SUPPORT`, `ROLE_ADMIN`) are collections of authorities assigned to a client; the approved mapping is ADR-016 (SAD 49.1, TDS 42). Implemented since `feature/security`: `security.RoleClaimAuthoritiesConverter` expands a JWT's `roles` claim (already `ROLE_`-prefixed) into granted authorities at authentication time, and every controller method that needs one carries its own `@PreAuthorize` - the single source of authorization truth; `security.SecurityConfig`'s own `authorizeHttpRequests` only distinguishes public (`/actuator/health`) from authenticated, never repeats an authority check.
- **Cross-module contracts stay security-neutral too, the same rule as the domain-type-leak rule above, applied to `Authentication`/`Jwt`/`Principal`.** `transaction.application.CreateTransactionCommand.actor()` is a plain `String` (the JWT `sub` claim), never a Spring Security or JWT type - `api.controller.TransactionController` extracts it via `java.security.Principal.getName()` (a JDK type, not a Spring Security one) rather than `@AuthenticationPrincipal Jwt`, so no business module or `api` controller needs to import `org.springframework.security.oauth2.jwt.Jwt` at all. Apply the same primitive-only boundary to any future authenticated-identity propagation.

## Testing Rules

Follow the testing strategy and the required-tests list in the Technical Design Specification (70) and SAD 45.

Prefer, in this order of value for this codebase:

1. **Unit tests** for domain and application logic (JUnit 5 + Mockito), no Spring context.
2. **Repository tests** against a real PostgreSQL via Testcontainers; never mock the database for persistence-layer tests.
3. **Integration tests** that exercise a use case end-to-end within the Spring context.
4. **Controller tests** for request validation, status codes, and error-response shape.
5. **Architecture tests**: Spring Modulith's `ApplicationModules.verify()` (`ModularityTests`) plus the hand-rolled `FrameworkIndependenceAssertions`-based checks (`<Module>ArchitectureTests`), both under the `architecture` test package, to make module boundary and layering violations a build failure, not a review comment.

Every new feature needs, at minimum: the happy path, each documented validation failure, and each documented error-code scenario for that endpoint/use case. Don't write a dedicated test purely to exercise a trivial getter.

## Documentation Rules

Whenever a major architectural decision changes (module boundaries, aggregate ownership, API contract, security model, database schema), update, in the same change:

- `README.md`
- The relevant SAD part(s) in `documentation/`
- The TDS
- The ADR register (SAD 49): add a new `ADR-0XX` entry; do not silently edit the rationale of an existing accepted ADR, mark it superseded instead

Documentation must never be allowed to drift behind the code. If a change to `documentation/` is out of scope for the current task, say so explicitly rather than skipping it silently.

## Documentation Precedence

When two documentation sources disagree, resolve the disagreement using this precedence order, highest first:

1. Accepted ADRs (the register in SAD 49)
2. The current approved SAD files: those whose filenames contain `v_2` (`Solution_Architecture_Document(SAD)_v_2_Part_*.md`). The `v_2` filename is what makes a part authoritative; `Solution_Architecture_Document(SAD)_v_1.md` is superseded and retained only for history.
3. The Technical Design Specification (TDS)
4. `README.md`
5. `CLAUDE.md`

Filenames are authoritative for document versions. Do not rename a SAD file or change its version identifier just because its historical wording reads differently from a later decision; supersede outdated content with a new accepted ADR (or a new versioned SAD part) instead of editing history in place.

Field-shape conflicts between SAD and TDS entity/DDL definitions have recurred for both `customer` and `merchant` (different field names, TDS omitting `version`, different `VARCHAR` lengths). The established resolution is: follow SAD (it outranks TDS, and its inclusion of `version` is also backed by Accepted ADR-014), and report the specific conflict rather than silently picking a side.

When sources conflict:

- Report the conflict rather than silently choosing one source.
- Identify the affected files and sections.
- Propose a resolution.
- Do not implement against the unresolved area until the conflict is recorded as resolved, normally via a new accepted ADR, per [Documentation Rules](#documentation-rules).

## Implementation Rules

Implement **one bounded context at a time**, in dependency order, each independently compilable and testable before moving to the next:

```
feature/project-structure → feature/shared → feature/customer → feature/merchant →
feature/categorisation → feature/audit → feature/transaction → feature/aggregation →
feature/api → feature/security
```

**`feature/project-structure` comes first and establishes only:**

- the package skeleton for every module (`domain`, `application`, `port`, `persistence`, etc., as listed under [Package Responsibilities](#package-responsibilities))
- Spring Modulith module boundaries (the package layout that lets `ApplicationModules.of(...)` discover each module)
- the architecture verification test(s) that enforce those boundaries
- shared cross-cutting configuration structure (e.g. where `config` and `security` will live)

It must not implement any business logic, entity, controller, repository, or service. It only creates the skeleton the later feature branches build inside.

After that, `shared`, `customer`, and `merchant` come next because nothing else in the domain depends on anything they don't already have. `categorisation` and `audit` are established **before** `transaction`, not after, because the transaction ingestion workflow itself depends on both: a transaction cannot be persisted as `PROCESSED` without a categorisation decision, and every ingestion outcome (success, duplicate, rejection) must be recorded as an audit event as part of that same use case. Building `transaction` first would mean building it against capabilities that don't exist yet. `aggregation` remains last because it only reads persisted transaction data through a read-only query port; there is nothing for it to summarise until `transaction` exists.

Do not start a module whose dependencies aren't yet in place, and do not let a feature branch grow to span multiple modules. That's a signal the module boundary needs re-examining, not a reason to skip the branch split.

**Current status:** `project-structure`, `shared`, `customer`, `merchant`, `categorisation`, `audit`, `transaction`, and `aggregation` are all complete - every module in the original sequence diagram. `categorisation` and `audit` were built as sibling branches off the same post-merchant `main` commit, since neither depends on the other; `transaction` resumed only once both were merged, since it genuinely depends on both. The original, once-reverted `feature/transaction` attempt was blocked by `transactions.category_id` being a `NOT NULL` foreign key to a `transaction_categories` table that didn't exist yet at that point - see the git history for the full reasoning, no longer relevant now that both prerequisites exist. `TransactionStatus`'s persisted values were revisited as planned: SAD's three-value version (`RECEIVED`, `PROCESSED`, `REJECTED`) was followed over TDS's five-value one (see [Documentation Conflicts](#documentation-precedence)), and in this branch's synchronous single-transaction flow, only `PROCESSED` is ever actually produced by `Transaction.register` - a validation failure, duplicate, unsupported source or missing customer is rejected via an exception before a `Transaction` is ever constructed, not by persisting a row with status `RECEIVED` or `REJECTED`. `aggregation` needed a read/query capability added to `transaction` that hadn't existed before (`TransactionQueryPort`, see [Ports and Adapters](#ports-and-adapters)) - exactly the dependency the original sequencing anticipated by building `aggregation` last. **`api` and `security` are now both complete too - every module in the original sequence diagram is implemented.** `feature/api` wired the already-built application use cases (`CreateTransactionUseCase`, the four `Get*SummaryUseCase`s) to real HTTP endpoints behind a deliberately temporary permit-all `SecurityFilterChain`, and along the way needed one genuine cross-module contract fix predicted by the "no module's application layer should need further changes" caveat above turning out to matter: `aggregation`'s four `Get*SummaryUseCase`s originally took `aggregation.domain.DateRange` directly, which `api` could not reference without exposing `aggregation.domain` cross-module (Spring Modulith would reject it) - fixed by changing those ports to take raw `LocalDate from, LocalDate to` instead, with `DateRange` construction/validation moved inside each `Get*SummaryService`, the exact same "primitives at the boundary" pattern already established for every other cross-module contract. `feature/security` then replaced the temporary posture with the real, documented JWT/RBAC model (SAD 36, TDS 41-44, ADR-007, ADR-016) and needed one further, smaller contract change: `transaction.application.CreateTransactionCommand` gained a plain `String actor` field (the JWT subject), replacing a hardcoded `"SYSTEM"` placeholder - `transaction.application` still never imports a Spring Security type, matching the same primitives-at-the-boundary discipline. Two real, scoped contract changes across two branches, each caught and fixed rather than assumed away - the caveat did its job.

**`feature/transaction-query` (post-MVP, after every branch in the original sequence diagram above) closed FR-08/UC-03/UC-04/TDS 30-31: `GET /api/v1/transactions/{id}` and `GET /api/v1/transactions` (filtered, paginated, sorted search), both `TRANSACTION_READ`-protected.** Bulk transaction creation, category administration, audit querying, observability, and OpenAPI remained explicitly out of scope. Read-side enrichment: `get` runs a fixed three-query flow (one `TransactionSearchRepositoryPort.findDetailById` intra-module join against `transaction_sources`, one `GetCategoryUseCase.get`, one `GetMerchantsUseCase.get` skipped when there's no merchant); `search` runs the same enrichment once per page, not once per row (see the batch-lookup bullet under [Module Boundaries and Dependency Rules](#module-boundaries-and-dependency-rules)). Sorting is deliberately restricted to exactly one whitelisted field (`transactionTimestamp` -> `occurredAt`, asc/desc) per the corrected plan - `amount`/`createdAt` are not sortable, since no authoritative contract documents them, and `TransactionSearchQuery` has no sort-field parameter at all so no other field can reach persistence even by mistake. `TransactionDetails` (get) and `TransactionSearchResultItem` (search) are deliberately kept as separate application-layer records from each other and from `TransactionCreatedResult` (create), per an explicit instruction to keep command/query contracts semantically distinct rather than unifying structurally-identical shapes into one `TransactionView` - inspection found no architectural reason strong enough to justify the unification, only the elimination of duplicate fields, which wasn't sufficient on its own. `V11__add_transaction_search_composite_indexes.sql` replaces the single-column `idx_transactions_category`/`idx_transactions_merchant` indexes with composite `(category_id, occurred_at)`/`(merchant_id, occurred_at)` ones, matching the new search query shapes. This branch needed no `ApplicationModule.allowedDependencies` changes: `transaction`'s existing `categorisation :: application`/`merchant :: application` entries already covered the new `GetCategoryUseCase`/`GetMerchantsUseCase` calls.
