# Technical Design Specification (TDS)

## Transaction Aggregation API

**Document Type:** Implementation Blueprint  
**Version:** 1.0  
**Author:** Tinyiko Chauke  
**Role:** Senior Software Engineer  
**Date:** July 2026  

---

## 1. Purpose

This Technical Design Specification translates the approved Solution Architecture Document into a detailed implementation blueprint.

Its purpose is to remove ambiguity before coding begins by defining:

- Package structure
- Module boundaries and dependencies
- Domain entities and value objects
- Database tables and constraints
- API contracts
- Categorisation rules
- Error codes
- Security roles
- Internal events
- Sequence flows
- Class catalogue

The document serves as the construction manual for the Transaction Aggregation API.

---

# Part 1 – Project Structure and Package Design

## 2. Proposed Repository Structure

```text
transaction-aggregation-api
├── docs
│   ├── architecture
│   ├── adr
│   └── diagrams
├── postman
├── src
│   ├── main
│   │   ├── java
│   │   │   └── za
│   │   │       └── co
│   │   │           └── transactionaggregation
│   │   │               ├── TransactionAggregationApplication.java
│   │   │               ├── api
│   │   │               │   ├── controller
│   │   │               │   ├── request
│   │   │               │   ├── response
│   │   │               │   └── advice
│   │   │               ├── transaction
│   │   │               │   ├── domain
│   │   │               │   ├── application
│   │   │               │   ├── port
│   │   │               │   ├── persistence
│   │   │               │   ├── mapper
│   │   │               │   ├── event
│   │   │               │   └── validation
│   │   │               ├── categorisation
│   │   │               │   ├── domain
│   │   │               │   ├── application
│   │   │               │   ├── port
│   │   │               │   ├── persistence
│   │   │               │   ├── rule
│   │   │               │   └── event
│   │   │               ├── aggregation
│   │   │               │   ├── domain
│   │   │               │   ├── application
│   │   │               │   ├── port
│   │   │               │   ├── persistence
│   │   │               │   └── mapper
│   │   │               ├── customer
│   │   │               │   ├── domain
│   │   │               │   ├── application
│   │   │               │   ├── port
│   │   │               │   └── persistence
│   │   │               ├── merchant
│   │   │               │   ├── domain
│   │   │               │   ├── application
│   │   │               │   ├── port
│   │   │               │   └── persistence
│   │   │               ├── audit
│   │   │               │   ├── domain
│   │   │               │   ├── application
│   │   │               │   ├── port
│   │   │               │   └── persistence
│   │   │               ├── security
│   │   │               ├── config
│   │   │               └── shared
│   │   │                   ├── exception
│   │   │                   ├── logging
│   │   │                   ├── validation
│   │   │                   ├── event
│   │   │                   └── util
│   │   └── resources
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       └── db
│   │           └── migration
│   └── test
│       └── java
│           └── za
│               └── co
│                   └── transactionaggregation
├── compose.yaml
├── Dockerfile
├── pom.xml
├── README.md
└── .gitignore
```

## 3. Package Responsibilities

### `api`

Contains HTTP-facing components only.

Responsibilities:

- REST controllers
- Request DTOs
- Response DTOs
- API-level validation
- Exception-to-response mapping

The API package must not contain business logic.

### `transaction`

Owns the transaction lifecycle.

Responsibilities:

- Receive transactions
- Validate transactions
- Normalise input
- Detect duplicates
- Persist transactions
- Retrieve transactions
- Publish transaction events

### `categorisation`

Owns transaction classification.

Responsibilities:

- Store category definitions
- Store categorisation rules
- Evaluate merchant and description rules
- Assign transaction categories
- Publish categorisation events

### `aggregation`

Owns financial calculations and reporting.

Responsibilities:

- Calculate income
- Calculate expenditure
- Calculate net cash flow
- Summarise by category
- Summarise by merchant
- Summarise by month

### `customer`

Owns customer reference data.

Responsibilities:

- Create or retrieve customers
- Validate customer existence
- Expose customer lookup interfaces

### `merchant`

Owns merchant reference data.

Responsibilities:

- Normalise merchant names
- Match known merchants
- Create merchant records when necessary

### `audit`

Owns auditable business and operational events.

Responsibilities:

- Persist audit records
- Record success and failure events
- Support traceability

### `security`

Contains authentication and authorization components.

Responsibilities:

- JWT validation
- Role extraction
- Endpoint security
- Access-denied handling

### `shared`

Contains reusable technical components only.

It must not contain business rules or domain ownership.

---

# Part 2 – Module Design

## 4. Transaction Module

### Responsibilities

- Accept a valid transaction command
- Validate transaction details
- Detect duplicate transactions
- Resolve customer and transaction source
- Request categorisation
- Persist the transaction
- Publish internal events

### Public Application Interfaces

```java
public interface CreateTransactionUseCase {
    TransactionResult create(CreateTransactionCommand command);
}
```

```java
public interface CreateTransactionsBulkUseCase {
    BulkTransactionResult create(List<CreateTransactionCommand> commands);
}
```

```java
public interface GetTransactionUseCase {
    TransactionView get(TransactionId transactionId);
}
```

```java
public interface SearchTransactionsUseCase {
    Page<TransactionView> search(TransactionSearchCriteria criteria);
}
```

### Dependencies

- Customer module
- Merchant module
- Categorisation module
- Audit module
- Shared technical components

### Events Published

- `TransactionReceivedEvent`
- `TransactionPersistedEvent`
- `DuplicateTransactionDetectedEvent`
- `TransactionRejectedEvent`

---

## 5. Categorisation Module

### Responsibilities

- Evaluate transaction categorisation rules
- Match merchant keywords
- Match transaction description keywords
- Assign a fallback category
- Return category decision and matching reason

### Public Interface

```java
public interface CategoriseTransactionUseCase {
    CategorisationDecision categorise(CategorisationInput input);
}
```

### Dependencies

- Merchant module
- Category repository
- Rule repository

### Events Published

- `TransactionCategorisedEvent`
- `TransactionUncategorisedEvent`

---

## 6. Aggregation Module

### Responsibilities

- Produce customer financial summaries
- Group transactions by category
- Group transactions by merchant
- Group transactions by month
- Calculate income, expenditure and net cash flow

### Public Interfaces

```java
public interface GetCustomerSummaryUseCase {
    CustomerSummaryView get(CustomerId customerId, DateRange dateRange);
}
```

```java
public interface GetCategorySummaryUseCase {
    List<CategorySummaryView> get(CustomerId customerId, DateRange dateRange);
}
```

```java
public interface GetMerchantSummaryUseCase {
    List<MerchantSummaryView> get(CustomerId customerId, DateRange dateRange);
}
```

```java
public interface GetMonthlySummaryUseCase {
    List<MonthlySummaryView> get(CustomerId customerId, DateRange dateRange);
}
```

### Dependencies

- Transaction query port
- Customer module

The aggregation module must not write transaction records.

---

## 7. Customer Module

### Responsibilities

- Own customer reference data
- Confirm customer existence
- Return customer details to other modules

### Public Interfaces

```java
public interface CustomerLookupPort {
    Customer getRequired(CustomerId customerId);
}
```

```java
public interface CustomerExistsPort {
    boolean exists(CustomerId customerId);
}
```

---

## 8. Audit Module

### Responsibilities

- Record significant business events
- Persist correlation identifiers
- Record actor, action, outcome and timestamp
- Support production diagnostics and traceability

### Public Interface

```java
public interface AuditEventPublisher {
    void publish(AuditRecord record);
}
```

---

# Part 3 – Domain Entities

## 9. Customer

| Field | Type | Required | Rules |
|---|---|---:|---|
| `id` | UUID | Yes | Generated by the platform |
| `externalCustomerId` | String | Yes | Unique, maximum 100 characters |
| `firstName` | String | Yes | Maximum 100 characters |
| `lastName` | String | Yes | Maximum 100 characters |
| `status` | CustomerStatus | Yes | Defaults to `ACTIVE` |
| `createdAt` | Instant | Yes | Generated |
| `updatedAt` | Instant | Yes | Generated |

### Customer Status

```text
ACTIVE
SUSPENDED
CLOSED
```

---

## 10. Transaction

| Field | Type | Required | Rules |
|---|---|---:|---|
| `id` | UUID | Yes | Generated by the platform |
| `externalTransactionId` | String | Yes | Unique per source |
| `customerId` | UUID | Yes | Must reference a valid customer |
| `sourceId` | UUID | Yes | Must reference a valid source |
| `merchantId` | UUID | No | Optional when merchant cannot be resolved |
| `categoryId` | UUID | Yes | Fallback category allowed |
| `amount` | BigDecimal | Yes | Greater than zero |
| `currency` | String | Yes | ISO 4217, defaults to `ZAR` |
| `direction` | TransactionDirection | Yes | `CREDIT` or `DEBIT` |
| `description` | String | Yes | Maximum 500 characters |
| `transactionTimestamp` | Instant | Yes | Must not be unreasonably future-dated |
| `status` | TransactionStatus | Yes | Defaults to `PROCESSED` |
| `createdAt` | Instant | Yes | Generated |
| `updatedAt` | Instant | Yes | Generated |

### Transaction Direction

```text
CREDIT
DEBIT
```

### Transaction Status

```text
RECEIVED
VALIDATED
PROCESSED
REJECTED
DUPLICATE
```

---

## 11. Transaction Source

| Field | Type | Required | Rules |
|---|---|---:|---|
| `id` | UUID | Yes | Generated |
| `code` | String | Yes | Unique, uppercase |
| `name` | String | Yes | Maximum 150 characters |
| `status` | SourceStatus | Yes | Defaults to `ACTIVE` |
| `createdAt` | Instant | Yes | Generated |

### Initial Sources

```text
MOCK_BANK_A
MOCK_BANK_B
MANUAL_UPLOAD
```

---

## 12. Merchant

| Field | Type | Required | Rules |
|---|---|---:|---|
| `id` | UUID | Yes | Generated |
| `canonicalName` | String | Yes | Unique normalised name |
| `displayName` | String | Yes | User-facing name |
| `createdAt` | Instant | Yes | Generated |
| `updatedAt` | Instant | Yes | Generated |

---

## 13. Transaction Category

| Field | Type | Required | Rules |
|---|---|---:|---|
| `id` | UUID | Yes | Generated |
| `code` | String | Yes | Unique |
| `name` | String | Yes | Unique display name |
| `type` | CategoryType | Yes | `INCOME`, `EXPENSE` or `OTHER` |
| `active` | Boolean | Yes | Defaults to `true` |
| `createdAt` | Instant | Yes | Generated |

### Initial Categories

```text
SALARY
GROCERIES
FUEL
RESTAURANTS
TRANSPORT
UTILITIES
ENTERTAINMENT
HEALTHCARE
INSURANCE
BANK_FEES
TRANSFER
OTHER_INCOME
OTHER_EXPENSE
UNCATEGORISED
```

---

## 14. Categorisation Rule

| Field | Type | Required | Rules |
|---|---|---:|---|
| `id` | UUID | Yes | Generated |
| `categoryId` | UUID | Yes | Valid category |
| `matchField` | MatchField | Yes | Merchant or description |
| `operator` | MatchOperator | Yes | Initial implementation uses `CONTAINS` |
| `matchValue` | String | Yes | Stored uppercase |
| `priority` | Integer | Yes | Lower number evaluated first |
| `active` | Boolean | Yes | Defaults to `true` |
| `createdAt` | Instant | Yes | Generated |

---

## 15. Audit Event

| Field | Type | Required | Rules |
|---|---|---:|---|
| `id` | UUID | Yes | Generated |
| `correlationId` | String | Yes | Used across request flow |
| `aggregateType` | String | Yes | Example: `TRANSACTION` |
| `aggregateId` | UUID | No | Optional for failures before creation |
| `eventType` | String | Yes | Event name |
| `outcome` | AuditOutcome | Yes | `SUCCESS` or `FAILURE` |
| `actor` | String | No | JWT subject or system |
| `details` | JSONB | No | Sanitised metadata |
| `createdAt` | Instant | Yes | Generated |

---

# Part 4 – Value Objects

## 16. Money

```java
public record Money(BigDecimal amount, Currency currency) {
}
```

Rules:

- Amount may not be null
- Currency may not be null
- Scale must not exceed two decimal places for ZAR
- Transaction input amount must be greater than zero
- Monetary arithmetic must use `BigDecimal`
- Floating-point types must never be used for money

---

## 17. TransactionId

```java
public record TransactionId(UUID value) {
}
```

Rules:

- Value may not be null
- Must represent a valid UUID

---

## 18. CustomerId

```java
public record CustomerId(UUID value) {
}
```

Rules:

- Value may not be null
- Must represent a valid UUID

---

## 19. ExternalTransactionId

```java
public record ExternalTransactionId(String value) {
}
```

Rules:

- Required
- Trimmed
- Maximum 100 characters
- Unique together with transaction source

---

## 20. DateRange

```java
public record DateRange(LocalDate from, LocalDate to) {
}
```

Rules:

- Both dates are required
- `from` may not be after `to`
- Maximum query range for synchronous API calls: 24 months

---

# Part 5 – Database Design

## 21. `customers`

```sql
CREATE TABLE customers (
    id UUID PRIMARY KEY,
    external_customer_id VARCHAR(100) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
```

---

## 22. `transaction_sources`

```sql
CREATE TABLE transaction_sources (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
```

---

## 23. `merchants`

```sql
CREATE TABLE merchants (
    id UUID PRIMARY KEY,
    canonical_name VARCHAR(200) NOT NULL UNIQUE,
    display_name VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
```

---

## 24. `transaction_categories`

```sql
CREATE TABLE transaction_categories (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL
);
```

---

## 25. `categorisation_rules`

```sql
CREATE TABLE categorisation_rules (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL,
    match_field VARCHAR(30) NOT NULL,
    operator VARCHAR(30) NOT NULL,
    match_value VARCHAR(200) NOT NULL,
    priority INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_rule_category
        FOREIGN KEY (category_id)
        REFERENCES transaction_categories(id),
    CONSTRAINT uq_categorisation_rule
        UNIQUE (category_id, match_field, match_value)
);
```

---

## 26. `transactions`

```sql
CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    external_transaction_id VARCHAR(100) NOT NULL,
    customer_id UUID NOT NULL,
    source_id UUID NOT NULL,
    merchant_id UUID,
    category_id UUID NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    description VARCHAR(500) NOT NULL,
    transaction_timestamp TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_transaction_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(id),

    CONSTRAINT fk_transaction_source
        FOREIGN KEY (source_id)
        REFERENCES transaction_sources(id),

    CONSTRAINT fk_transaction_merchant
        FOREIGN KEY (merchant_id)
        REFERENCES merchants(id),

    CONSTRAINT fk_transaction_category
        FOREIGN KEY (category_id)
        REFERENCES transaction_categories(id),

    CONSTRAINT uq_transaction_source_external_id
        UNIQUE (source_id, external_transaction_id),

    CONSTRAINT chk_transaction_amount
        CHECK (amount > 0),

    CONSTRAINT chk_transaction_currency
        CHECK (currency ~ '^[A-Z]{3}$'),

    CONSTRAINT chk_transaction_direction
        CHECK (direction IN ('CREDIT', 'DEBIT'))
);
```

### Required Indexes

```sql
CREATE INDEX idx_transactions_customer_timestamp
    ON transactions(customer_id, transaction_timestamp);

CREATE INDEX idx_transactions_category
    ON transactions(category_id);

CREATE INDEX idx_transactions_merchant
    ON transactions(merchant_id);

CREATE INDEX idx_transactions_source
    ON transactions(source_id);

CREATE INDEX idx_transactions_status
    ON transactions(status);
```

---

## 27. `audit_events`

```sql
CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    correlation_id VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id UUID,
    event_type VARCHAR(100) NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    actor VARCHAR(150),
    details JSONB,
    created_at TIMESTAMPTZ NOT NULL
);
```

### Required Indexes

```sql
CREATE INDEX idx_audit_events_correlation_id
    ON audit_events(correlation_id);

CREATE INDEX idx_audit_events_aggregate
    ON audit_events(aggregate_type, aggregate_id);

CREATE INDEX idx_audit_events_created_at
    ON audit_events(created_at);
```

---

# Part 6 – API Contract

## 28. Create Transaction

### Endpoint

```http
POST /api/v1/transactions
```

### Required Role

```text
ROLE_API_CONSUMER
ROLE_ADMIN
```

### Request

```json
{
  "externalTransactionId": "EXT-2026-000001",
  "customerId": "4ab90b40-27bc-4e91-b5a5-76afedb93496",
  "sourceCode": "MOCK_BANK_A",
  "amount": 1250.50,
  "currency": "ZAR",
  "direction": "DEBIT",
  "description": "CHECKERS CENTURION",
  "merchantName": "Checkers",
  "transactionTimestamp": "2026-07-30T10:15:30Z"
}
```

### Response

```json
{
  "transactionId": "063ae5e3-ec75-43c4-986d-b22d08af9c61",
  "externalTransactionId": "EXT-2026-000001",
  "status": "PROCESSED",
  "category": {
    "code": "GROCERIES",
    "name": "Groceries"
  },
  "createdAt": "2026-07-30T10:15:31Z"
}
```

### HTTP Status

```text
201 Created
```

### Validation Rules

- `externalTransactionId` is required
- `customerId` must be a UUID
- `sourceCode` is required
- `amount` must be greater than zero
- `currency` must be a valid three-letter currency code
- `direction` must be `CREDIT` or `DEBIT`
- `description` is required
- `transactionTimestamp` is required

---

## 29. Bulk Create Transactions

### Endpoint

```http
POST /api/v1/transactions/bulk
```

### Rules

- Maximum 500 transactions per request
- Each item is processed independently
- The response contains per-item success or failure
- Duplicate items return a conflict result for that item
- One invalid item must not roll back all successful items

---

## 30. Get Transaction

```http
GET /api/v1/transactions/{transactionId}
```

Returns a single transaction.

---

## 31. Search Transactions

```http
GET /api/v1/transactions
```

### Query Parameters

```text
customerId
from
to
direction
categoryCode
merchantId
status
page
size
sort
```

### Defaults

```text
page=0
size=20
sort=transactionTimestamp,desc
```

### Limits

```text
Maximum page size: 100
Maximum date range: 24 months
```

---

## 32. Customer Summary

```http
GET /api/v1/customers/{customerId}/summary?from=2026-07-01&to=2026-07-31
```

### Response

```json
{
  "customerId": "4ab90b40-27bc-4e91-b5a5-76afedb93496",
  "period": {
    "from": "2026-07-01",
    "to": "2026-07-31"
  },
  "totalIncome": {
    "amount": 50000.00,
    "currency": "ZAR"
  },
  "totalExpenditure": {
    "amount": 32125.75,
    "currency": "ZAR"
  },
  "netCashFlow": {
    "amount": 17874.25,
    "currency": "ZAR"
  },
  "transactionCount": 82
}
```

---

## 33. Category Summary

```http
GET /api/v1/customers/{customerId}/categories
```

Groups debit transactions by category.

---

## 34. Merchant Summary

```http
GET /api/v1/customers/{customerId}/merchants
```

Groups debit transactions by merchant.

---

## 35. Monthly Summary

```http
GET /api/v1/customers/{customerId}/monthly-summary
```

Returns income, expenditure and net cash flow grouped by month.

---

# Part 7 – Categorisation Rules

## 36. Rule Evaluation Order

Rules are evaluated in ascending priority order.

```text
1. Direction-specific rules
2. Exact merchant matches
3. Merchant keyword rules
4. Description keyword rules
5. Fallback category
```

The first active matching rule wins.

---

## 37. Initial Categorisation Matrix

| Priority | Match Field | Keywords | Direction | Category |
|---:|---|---|---|---|
| 10 | Description | SALARY, PAYROLL, WAGES | CREDIT | SALARY |
| 20 | Merchant | CHECKERS, PICK N PAY, SHOPRITE, SPAR, WOOLWORTHS FOOD | DEBIT | GROCERIES |
| 30 | Merchant | SHELL, BP, ENGEN, SASOL | DEBIT | FUEL |
| 40 | Merchant | UBER, BOLT, GAUTRAIN | DEBIT | TRANSPORT |
| 50 | Merchant | ESKOM, CITY POWER, MUNICIPALITY, TELKOM, VODACOM, MTN | DEBIT | UTILITIES |
| 60 | Merchant | NETFLIX, SHOWMAX, SPOTIFY, DSTV | DEBIT | ENTERTAINMENT |
| 70 | Merchant | DIS-CHEM, CLICKS, MEDICLINIC, NETCARE | DEBIT | HEALTHCARE |
| 80 | Description | INSURANCE, PREMIUM | DEBIT | INSURANCE |
| 90 | Description | BANK FEE, SERVICE FEE, MONTHLY FEE | DEBIT | BANK_FEES |
| 100 | Description | TRANSFER, PAYMENT RECEIVED | CREDIT | OTHER_INCOME |
| 110 | Description | TRANSFER, EFT | DEBIT | TRANSFER |
| 999 | Any | No match | CREDIT | OTHER_INCOME |
| 1000 | Any | No match | DEBIT | UNCATEGORISED |

---

## 38. Merchant Normalisation

Before matching:

1. Trim leading and trailing spaces
2. Convert to uppercase
3. Replace repeated whitespace with one space
4. Remove selected punctuation
5. Preserve the original display value separately

Example:

```text
"  Checkers #104 Centurion "
```

Normalised to:

```text
CHECKERS 104 CENTURION
```

---

# Part 8 – Error Catalogue

## 39. Standard Error Response

The API uses RFC 9457 Problem Details.

```json
{
  "type": "https://errors.transaction-aggregation.local/TRX-001",
  "title": "Duplicate transaction",
  "status": 409,
  "detail": "A transaction with the supplied source and external transaction ID already exists.",
  "instance": "/api/v1/transactions",
  "errorCode": "TRX-001",
  "correlationId": "d91b836b-6137-4c8c-a7ec-534ac69f4680",
  "timestamp": "2026-07-30T10:15:31Z"
}
```

---

## 40. Error Codes

| Code | HTTP Status | Title | Cause |
|---|---:|---|---|
| `TRX-001` | 409 | Duplicate transaction | Source and external ID already exist |
| `TRX-002` | 400 | Invalid transaction amount | Amount is null, zero or negative |
| `TRX-003` | 404 | Transaction not found | Transaction ID does not exist |
| `TRX-004` | 400 | Invalid transaction direction | Unsupported direction |
| `TRX-005` | 400 | Invalid transaction timestamp | Timestamp missing or invalid |
| `TRX-006` | 422 | Transaction processing failed | Business processing could not complete |
| `CUS-001` | 404 | Customer not found | Customer ID does not exist |
| `SRC-001` | 404 | Transaction source not found | Source code does not exist |
| `CAT-001` | 404 | Category not found | Category does not exist |
| `CAT-002` | 422 | Categorisation failed | No valid categorisation decision produced |
| `AGG-001` | 400 | Invalid date range | Start date is after end date |
| `AGG-002` | 400 | Date range too large | Query period exceeds 24 months |
| `SEC-001` | 401 | Authentication required | Token missing or invalid |
| `SEC-002` | 403 | Access denied | User lacks permission |
| `VAL-001` | 400 | Validation failed | Request field validation failed |
| `SYS-001` | 500 | Internal server error | Unexpected internal failure |
| `SYS-002` | 503 | Service unavailable | Database or dependency unavailable |

---

# Part 9 – Security Model

## 41. Authentication

The API uses JWT Bearer authentication.

Expected claims:

```json
{
  "sub": "api-client-001",
  "roles": [
    "ROLE_API_CONSUMER"
  ],
  "customerId": "4ab90b40-27bc-4e91-b5a5-76afedb93496",
  "iss": "transaction-aggregation-auth",
  "aud": "transaction-aggregation-api",
  "exp": 1785405600
}
```

At authentication time, each value in the JWT `roles` claim is expanded into its mapped set of Spring Security granted authorities (see 42) before any authorization check runs. Endpoint and method security (`@PreAuthorize`) evaluate authorities only — role names are never checked directly. See ADR-016 (SAD 49.1).

---

## 42. Roles and Authority Mapping

Roles are named, coarse-grained collections of the fine-grained authorities defined in the SAD (36.4: `TRANSACTION_READ`, `TRANSACTION_WRITE`, `CUSTOMER_READ`, `AGGREGATION_READ`, `CATEGORY_ADMIN`, `AUDIT_READ`, `OPERATIONS_READ`). A role is what gets assigned to a client or user; an authority is what gets checked by `@PreAuthorize`. This mapping is the approved baseline from ADR-016.

### `ROLE_API_CONSUMER`

Authorities:

- `TRANSACTION_READ`
- `TRANSACTION_WRITE`
- `AGGREGATION_READ`

Effective permissions: create transactions, create bulk transactions, read transactions, read customer/aggregation summaries.

### `ROLE_SUPPORT`

Authorities:

- `TRANSACTION_READ`
- `CUSTOMER_READ`
- `AGGREGATION_READ`

Effective permissions: read transactions, read customer data, read aggregation summaries. Cannot create or modify transactions (no `TRANSACTION_WRITE`) and has no audit access (no `AUDIT_READ`).

### `ROLE_ADMIN`

Authorities:

- `TRANSACTION_READ`
- `TRANSACTION_WRITE`
- `CUSTOMER_READ`
- `AGGREGATION_READ`
- `CATEGORY_ADMIN`
- `AUDIT_READ`
- `OPERATIONS_READ`

Effective permissions: all API consumer and support permissions, manage categories and categorisation rules, read audit information, and access selected operational endpoints.

---

## 43. Endpoint Authorization Matrix

| Endpoint | API Consumer | Support | Admin |
|---|:---:|:---:|:---:|
| `POST /api/v1/transactions` | Yes | No | Yes |
| `POST /api/v1/transactions/bulk` | Yes | No | Yes |
| `GET /api/v1/transactions/**` | Yes | Yes | Yes |
| `GET /api/v1/customers/*/summary` | Yes | Yes | Yes |
| `GET /api/v1/customers/*/categories` | Yes | Yes | Yes |
| `GET /api/v1/customers/*/merchants` | Yes | Yes | Yes |
| `GET /api/v1/customers/*/monthly-summary` | Yes | Yes | Yes |
| `/actuator/health` | Public or infrastructure restricted | Public or infrastructure restricted | Public or infrastructure restricted |
| `/actuator/metrics` | No | No | Yes |

---

## 44. Security Rules

- JWT signature must be validated
- Token expiry must be validated
- Issuer and audience must be validated
- JWT `roles` claims must be expanded into granted authorities using the approved mapping (42, ADR-016) before any authorization decision is made
- Unauthorized requests return `401`
- Authenticated but forbidden requests return `403`
- Sensitive fields must not appear in logs
- Secrets must come from environment variables or a secret manager
- CSRF may be disabled for stateless APIs
- CORS must be explicitly configured
- Actuator exposure must be restricted

---

# Part 10 – Internal Event Catalogue

## 45. Event Envelope

```java
public record DomainEventEnvelope<T>(
    UUID eventId,
    String eventType,
    Instant occurredAt,
    String correlationId,
    T payload
) {
}
```

---

## 46. `TransactionReceivedEvent`

### Publisher

Transaction module

### Consumers

- Audit module
- Future observability or event-streaming adapter

### Payload

```json
{
  "externalTransactionId": "EXT-2026-000001",
  "customerId": "4ab90b40-27bc-4e91-b5a5-76afedb93496",
  "sourceCode": "MOCK_BANK_A",
  "receivedAt": "2026-07-30T10:15:30Z"
}
```

---

## 47. `TransactionPersistedEvent`

### Publisher

Transaction module

### Consumers

- Audit module
- Future reporting projections
- Future Kafka publisher

### Payload

```json
{
  "transactionId": "063ae5e3-ec75-43c4-986d-b22d08af9c61",
  "customerId": "4ab90b40-27bc-4e91-b5a5-76afedb93496",
  "categoryCode": "GROCERIES",
  "status": "PROCESSED",
  "persistedAt": "2026-07-30T10:15:31Z"
}
```

---

## 48. `DuplicateTransactionDetectedEvent`

### Publisher

Transaction module

### Consumers

- Audit module
- Future fraud or monitoring capability

### Payload

```json
{
  "externalTransactionId": "EXT-2026-000001",
  "sourceCode": "MOCK_BANK_A",
  "existingTransactionId": "063ae5e3-ec75-43c4-986d-b22d08af9c61",
  "detectedAt": "2026-07-30T10:15:31Z"
}
```

---

## 49. `TransactionCategorisedEvent`

### Publisher

Categorisation module

### Consumers

- Transaction module
- Audit module

### Payload

```json
{
  "transactionId": "063ae5e3-ec75-43c4-986d-b22d08af9c61",
  "categoryCode": "GROCERIES",
  "matchedRuleId": "c82e56ef-eb8c-45aa-ac72-1b1a04b63963",
  "reason": "Merchant contained CHECKERS",
  "categorisedAt": "2026-07-30T10:15:31Z"
}
```

---

## 50. `TransactionRejectedEvent`

### Publisher

Transaction module

### Consumers

- Audit module
- Future monitoring adapter

### Payload

```json
{
  "externalTransactionId": "EXT-2026-000001",
  "errorCode": "TRX-002",
  "reason": "Transaction amount must be greater than zero",
  "rejectedAt": "2026-07-30T10:15:31Z"
}
```

---

# Part 11 – Sequence Flows

## 51. Create Transaction Flow

```text
Client
  │
  ▼
TransactionController
  │ Validate API request
  ▼
CreateTransactionService
  │
  ├── Resolve Customer
  ├── Resolve Transaction Source
  ├── Validate Business Rules
  ├── Check Duplicate
  ├── Normalise Merchant
  ├── Request Categorisation
  ├── Create Transaction Domain Object
  ├── Persist Transaction
  ├── Publish TransactionPersistedEvent
  └── Record Audit Event
  │
  ▼
TransactionResponse
```

---

## 52. Duplicate Transaction Flow

```text
Client
  │
  ▼
TransactionController
  │
  ▼
CreateTransactionService
  │
  ▼
DuplicateTransactionChecker
  │
  ├── Existing transaction found
  ├── Publish DuplicateTransactionDetectedEvent
  ├── Record audit failure
  └── Throw DuplicateTransactionException
  │
  ▼
GlobalExceptionHandler
  │
  ▼
409 Conflict
```

---

## 53. Customer Summary Flow

```text
Client
  │
  ▼
AggregationController
  │ Validate customer ID and date range
  ▼
GetCustomerSummaryService
  │
  ├── Validate customer exists
  ├── Query credit total
  ├── Query debit total
  ├── Calculate net cash flow
  ├── Count transactions
  └── Build summary
  │
  ▼
CustomerSummaryResponse
```

---

# Part 12 – Class Catalogue

## 54. Controllers

```text
TransactionController
AggregationController
CategoryAdminController
```

---

## 55. Request DTOs

```text
CreateTransactionRequest
BulkCreateTransactionsRequest
TransactionSearchRequest
DateRangeRequest
CreateCategorisationRuleRequest
UpdateCategorisationRuleRequest
```

---

## 56. Response DTOs

```text
TransactionResponse
BulkTransactionResponse
BulkTransactionItemResponse
CustomerSummaryResponse
CategorySummaryResponse
MerchantSummaryResponse
MonthlySummaryResponse
CategoryResponse
ProblemDetailsResponse
PagedResponse<T>
```

---

## 57. Application Services

```text
CreateTransactionService
BulkCreateTransactionsService
GetTransactionService
SearchTransactionsService
CategoriseTransactionService
GetCustomerSummaryService
GetCategorySummaryService
GetMerchantSummaryService
GetMonthlySummaryService
CustomerLookupService
MerchantResolutionService
AuditService
```

---

## 58. Domain Entities

```text
Customer
Transaction
TransactionSource
Merchant
TransactionCategory
CategorisationRule
AuditEvent
```

---

## 59. Value Objects

```text
Money
TransactionId
CustomerId
MerchantId
CategoryId
ExternalTransactionId
DateRange
CorrelationId
```

---

## 60. Ports

```text
TransactionRepositoryPort
TransactionQueryPort
CustomerRepositoryPort
CustomerLookupPort
MerchantRepositoryPort
MerchantResolutionPort
CategoryRepositoryPort
CategorisationRuleRepositoryPort
AuditRepositoryPort
DomainEventPublisher
CurrentUserProvider
ClockProvider
```

---

## 61. Persistence Adapters

```text
JpaTransactionRepositoryAdapter
JpaCustomerRepositoryAdapter
JpaMerchantRepositoryAdapter
JpaCategoryRepositoryAdapter
JpaCategorisationRuleRepositoryAdapter
JpaAuditRepositoryAdapter
```

---

## 62. JPA Repositories

```text
SpringDataTransactionRepository
SpringDataCustomerRepository
SpringDataMerchantRepository
SpringDataCategoryRepository
SpringDataCategorisationRuleRepository
SpringDataAuditRepository
SpringDataTransactionSourceRepository
```

---

## 63. Mappers

```text
TransactionApiMapper
TransactionPersistenceMapper
CustomerMapper
MerchantMapper
CategoryMapper
AggregationResponseMapper
AuditMapper
```

---

## 64. Validators and Rules

```text
TransactionRequestValidator
TransactionBusinessValidator
DuplicateTransactionChecker
MerchantNormaliser
CategorisationRuleEngine
DirectionValidationRule
TimestampValidationRule
DateRangeValidator
```

---

## 65. Exceptions

```text
DuplicateTransactionException
TransactionNotFoundException
InvalidTransactionException
CustomerNotFoundException
TransactionSourceNotFoundException
CategoryNotFoundException
CategorisationException
InvalidDateRangeException
AccessDeniedBusinessException
InfrastructureException
```

---

## 66. Events

```text
TransactionReceivedEvent
TransactionPersistedEvent
DuplicateTransactionDetectedEvent
TransactionCategorisedEvent
TransactionUncategorisedEvent
TransactionRejectedEvent
```

---

## 67. Configuration Classes

```text
SecurityConfig
OpenApiConfig
JacksonConfig
JpaConfig
ClockConfig
CorrelationIdFilter
JwtAuthenticationConverter
ActuatorSecurityConfig
```

---

# 68. Module Dependency Rules

The following rules are mandatory:

1. Controllers may depend on application use cases.
2. Controllers may not depend directly on repositories.
3. Domain packages may not depend on Spring.
4. A module may not access another module's JPA repository.
5. Cross-module communication must use public interfaces or domain events.
6. Persistence entities must not be returned by controllers.
7. Aggregation may read transaction data only through `TransactionQueryPort`.
8. Shared packages may not contain domain-specific business logic.
9. Security logic must not be embedded in domain entities.
10. ArchUnit tests must enforce these rules.

---

# 69. Definition of Ready for Coding

Coding may begin when the following are approved:

- Package structure
- Module responsibilities
- Domain entities
- Value objects
- Database schema
- API contracts
- Categorisation rules
- Error catalogue
- Security model
- Internal events
- Class catalogue
- Module dependency rules

---

# 70. Recommended First Implementation Slice

The first implementation slice should deliver one complete end-to-end capability:

```text
POST /api/v1/transactions
    → authenticate
    → validate request
    → resolve customer and source
    → detect duplicate
    → normalise merchant
    → categorise
    → persist
    → publish internal event
    → record audit event
    → return 201 Created
```

Required tests for the first slice:

- Valid transaction is created
- Invalid amount is rejected
- Missing customer is rejected
- Unknown source is rejected
- Duplicate transaction returns `409`
- Merchant is normalised
- Correct category is assigned
- Fallback category is assigned
- Audit event is written
- API response matches the contract
- PostgreSQL integration test passes using Testcontainers

---

# 71. Conclusion

This Technical Design Specification defines the exact implementation blueprint for the Transaction Aggregation API.

The architecture is no longer conceptual. Package boundaries, module responsibilities, database structures, API contracts, security rules, categorisation logic, internal events and implementation classes are explicitly defined.

The next step is to convert this specification into an ordered implementation backlog and then create the Spring Boot repository.
