# Transaction Aggregation API
# Solution Architecture Document (SAD)

**Version:** 3.0  
**Part 2 – Requirements**

---

# 13. Functional Requirements

Each functional requirement is uniquely identified to support traceability from business need through implementation and testing.

| ID | Requirement |
|----|-------------|
| FR-01 | Ingest transactions through REST APIs from multiple mock providers. |
| FR-02 | Support single and bulk transaction submission. |
| FR-03 | Validate mandatory fields, identifiers, amounts, currencies and timestamps. |
| FR-04 | Detect duplicate transactions using the transaction source and external transaction identifier. |
| FR-05 | Normalise incoming payloads into a common transaction model. |
| FR-06 | Persist validated transactions in PostgreSQL. |
| FR-07 | Categorise transactions using configurable business rules. |
| FR-08 | Retrieve transactions using filtering, pagination and sorting. |
| FR-09 | Produce customer financial summaries. |
| FR-10 | Produce category, merchant and monthly summaries. |
| FR-11 | Expose versioned REST APIs documented with OpenAPI. |
| FR-12 | Record audit events for traceability. |
| FR-13 | Expose health, readiness and metrics endpoints. |
| FR-14 | Return consistent RFC 9457 Problem Details error responses. |

---

# 14. Non-Functional Requirements

| ID | Quality Attribute | Requirement |
|----|-------------------|-------------|
| NFR-01 | Performance | Summary APIs should typically respond within 500 ms under normal load. |
| NFR-02 | Reliability | Transaction processing must be idempotent where applicable. |
| NFR-03 | Availability | Health endpoints shall support operational monitoring. |
| NFR-04 | Security | JWT authentication and RBAC shall protect secured endpoints. |
| NFR-05 | Maintainability | Follow Clean Architecture, SOLID and modular design. |
| NFR-06 | Scalability | Stateless services supporting horizontal scaling. |
| NFR-07 | Testability | Business logic must be unit testable and integration tested. |
| NFR-08 | Observability | Structured logging, metrics, correlation IDs and audit events. |
| NFR-09 | Portability | Deployable through Docker with externalised configuration. |
| NFR-10 | Extensibility | New transaction sources and rules should be added with minimal impact. |
| NFR-11 | Data Integrity | Enforce referential integrity and business constraints. |
| NFR-12 | Documentation | Maintain architecture, API and deployment documentation. |

---

# 15. Use Cases

## UC-01 – Create Transaction

**Primary Actor:** API Consumer

**Preconditions**
- Valid JWT.
- Customer exists.
- Transaction source exists.

**Main Flow**
1. Submit transaction.
2. Validate request.
3. Detect duplicates.
4. Normalise merchant.
5. Categorise transaction.
6. Persist transaction.
7. Record audit event.
8. Return HTTP 201.

**Alternate Flows**
- Duplicate transaction → HTTP 409.
- Validation failure → HTTP 400.
- Customer not found → HTTP 404.

---

## UC-02 – Bulk Create Transactions

Actor: API Consumer

Outcome:
- Up to 500 transactions processed independently.
- Partial success is supported.
- Invalid items do not roll back valid items.

---

## UC-03 – Retrieve Transaction

Actor: API Consumer

Outcome:
- Retrieve a transaction by ID.

---

## UC-04 – Search Transactions

Actor: API Consumer

Supports:
- Date range
- Category
- Merchant
- Direction
- Status
- Pagination
- Sorting

---

## UC-05 – Customer Financial Summary

Actor: API Consumer

Outputs:
- Total income
- Total expenditure
- Net cash flow
- Transaction count

---

## UC-06 – Category Summary

Returns customer expenditure grouped by category.

---

## UC-07 – Merchant Summary

Returns customer expenditure grouped by merchant.

---

## UC-08 – Monthly Summary

Returns monthly financial summaries.

---

# 16. Business Rules

## Transaction Rules

- BR-01 Every transaction must belong to one customer.
- BR-02 Every transaction must originate from one transaction source.
- BR-03 Amount must be greater than zero.
- BR-04 Currency must be a valid ISO-4217 code.
- BR-05 Transactions become immutable after successful processing.
- BR-06 External transaction identifiers must be unique per source.

## Duplicate Detection Rules

- BR-07 Duplicate = same source + same external transaction identifier.
- BR-08 Duplicate transactions are rejected with HTTP 409.

## Categorisation Rules

- BR-09 First matching active rule wins.
- BR-10 Merchant rules are evaluated before description rules.
- BR-11 If no rule matches, assign the fallback category.

## Aggregation Rules

- BR-12 Credits contribute to income.
- BR-13 Debits contribute to expenditure.
- BR-14 Net cash flow = Income − Expenditure.
- BR-15 Reporting is generated from persisted transactions only.

---

# 17. Requirements Traceability Matrix

| Requirement | Module | Primary API | Database |
|-------------|--------|-------------|----------|
| FR-01 | Ingestion | POST /transactions | transactions |
| FR-02 | Transaction | POST /transactions/bulk | transactions |
| FR-03 | Transaction | POST /transactions | transactions |
| FR-04 | Transaction | POST /transactions | transactions |
| FR-05 | Transaction | POST /transactions | transactions |
| FR-06 | Transaction | POST /transactions | transactions |
| FR-07 | Categorisation | POST /transactions | categorisation_rules |
| FR-08 | Transaction | GET /transactions | transactions |
| FR-09 | Aggregation | GET /customers/{id}/summary | transactions |
| FR-10 | Aggregation | Summary APIs | transactions |
| FR-11 | API | All REST endpoints | N/A |
| FR-12 | Audit | All APIs | audit_events |
| FR-13 | Operations | /actuator/* | N/A |
| FR-14 | Shared | All APIs | N/A |

---

## End of Part 2

The next section introduces the complete architecture, C4 diagrams, module interactions and component responsibilities.
