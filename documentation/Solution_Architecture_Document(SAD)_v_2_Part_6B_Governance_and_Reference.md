
# Transaction Aggregation API
# Solution Architecture Document (SAD)

**Version:** 3.0  
**Part 6B – Governance & Reference**

---

# 49. Architecture Decision Records (ADRs)

Architecture Decision Records capture significant technical decisions together with their rationale.

| ADR | Decision | Status | Rationale |
|-----|----------|--------|-----------|
| ADR-001 | Modular Monolith | Accepted | Delivers modularity without distributed-system complexity. |
| ADR-002 | Domain-Driven Design | Accepted | Aligns software structure with business capabilities. |
| ADR-003 | Clean Architecture | Accepted | Keeps business rules independent of frameworks. |
| ADR-004 | Spring Boot | Accepted | Mature Java ecosystem with strong production support. |
| ADR-005 | PostgreSQL | Accepted | ACID guarantees, indexing and JSONB support. |
| ADR-006 | Spring Data JPA | Accepted | Reduces persistence boilerplate. |
| ADR-007 | JWT Authentication | Accepted | Stateless API security. |
| ADR-008 | Docker | Accepted | Consistent deployments across environments. |
| ADR-009 | Flyway | Accepted | Controlled database versioning. |
| ADR-010 | OpenAPI | Accepted | Self-documenting REST APIs. |
| ADR-011 | Structured JSON Logging | Accepted | Improves operational observability. |
| ADR-012 | RFC 9457 Problem Details | Accepted | Standardised error responses. |
| ADR-013 | Immutable Transactions | Accepted | Preserves financial integrity and auditability. |
| ADR-014 | Optimistic Locking | Accepted | Prevents lost updates with minimal locking overhead. |
| ADR-015 | Future Microservice Evolution | Accepted | Enables gradual extraction of bounded contexts. |
| ADR-016 | RBAC Role-to-Authority Mapping | Accepted | Roles are named collections of fine-grained authorities; endpoint and method security evaluate authorities only. Resolves the SAD/TDS role-versus-authority naming conflict — see §49.1. |

Decision lifecycle:

- Proposed
- Accepted
- Superseded
- Deprecated

---

## 49.1 ADR-016 Detail — Roles, Authorities and the Approved Mapping

**Context:** Part 5 of this document (§36.4) defines fine-grained authorities that protect individual operations, while the Technical Design Specification (§42) independently defines coarser roles assigned to clients. Neither document originally stated how the two relate, which was tracked as an unresolved documentation conflict.

**Decision:**

- **Authorities** are the unit of protection for individual operations. They are evaluated by Spring Security method security (`@PreAuthorize`) and are never assigned to a user or client directly.
- **Roles** are named, assignable collections of authorities, granted to a user or client. A client is assigned one or more roles; it never holds a bare authority outside of a role.
- At authentication time, each JWT `roles` claim value is expanded into its mapped set of Spring Security `GrantedAuthority` instances before any authorization check runs. `@PreAuthorize` expressions check authorities (for example `hasAuthority('TRANSACTION_WRITE')`), never role names.

**Approved initial role-to-authority mapping:**

| Role | Authorities |
|---|---|
| `ROLE_API_CONSUMER` | `TRANSACTION_READ`, `TRANSACTION_WRITE`, `AGGREGATION_READ` |
| `ROLE_SUPPORT` | `TRANSACTION_READ`, `CUSTOMER_READ`, `AGGREGATION_READ` |
| `ROLE_ADMIN` | `TRANSACTION_READ`, `TRANSACTION_WRITE`, `CUSTOMER_READ`, `AGGREGATION_READ`, `CATEGORY_ADMIN`, `AUDIT_READ`, `OPERATIONS_READ` |

**Consequences:** the TDS (§42) documents the role layer and its mapping to authorities; the SAD (§36.4) documents the authority layer; this ADR documents the relationship and the initial mapping between them. This mapping is the approved starting point for the Spring Security configuration, which is implemented separately and remains out of scope for this ADR.

**Status:** Accepted.

---

# 50. Risks

## 50.1 Technical Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Poor database indexing | Slow queries | Query tuning and index reviews |
| Uncontrolled module coupling | Reduced maintainability | Enforce module boundaries |
| Duplicate transaction ingestion | Financial inconsistency | Idempotency and unique constraints |
| Performance degradation | Poor user experience | Load testing and monitoring |
| Security vulnerabilities | Data exposure | Secure coding, dependency scanning and reviews |
| Configuration drift | Deployment failures | Infrastructure as Code and externalised configuration |
| Dependency vulnerabilities | Operational risk | Continuous dependency scanning |

## 50.2 Operational Risks

| Risk | Mitigation |
|------|------------|
| Database outage | Backup, monitoring and recovery procedures |
| Infrastructure failure | Container restart policies and redundancy |
| Log storage exhaustion | Log retention and rotation |
| Secret exposure | Managed secret storage and least privilege |
| Deployment failure | Automated rollback and health verification |

## 50.3 Business Risks

| Risk | Mitigation |
|------|------------|
| Requirement changes | Iterative delivery and modular design |
| Regulatory changes | Configurable business rules and versioned APIs |
| Data growth | Horizontal scaling and database optimisation |

---

# 51. Future Improvements

Planned enhancements include:

## Functional

- Multi-currency support
- Scheduled recurring reports
- User-defined categorisation rules
- Import from additional financial institutions
- Notification and subscription services

## Technical

- Distributed caching
- Event-driven integration
- CQRS read models
- Search optimisation
- Materialised reporting views
- Multi-region deployment

## Platform

- Kubernetes deployment
- Service mesh
- Centralised API gateway
- Distributed tracing across services
- Automated blue-green deployments

---

# 52. Technology Stack

## Backend

| Technology | Purpose |
|-----------|---------|
| Java 21 | Programming language |
| Spring Boot | Application framework |
| Spring Web | REST APIs |
| Spring Security | Authentication and authorisation |
| Spring Data JPA | Persistence |
| Hibernate | ORM |
| Flyway | Database migrations |
| Maven | Build automation |
| PostgreSQL | Relational database |

## Quality & Testing

| Technology | Purpose |
|-----------|---------|
| JUnit 5 | Unit testing |
| Mockito | Mocking |
| Testcontainers | Integration testing |
| Spring Boot Test | Application testing |
| JaCoCo | Code coverage |

## Operations

| Technology | Purpose |
|-----------|---------|
| Docker | Containerisation |
| Docker Compose | Local orchestration |
| Micrometer | Metrics |
| Spring Boot Actuator | Operational endpoints |
| OpenAPI / Swagger | API documentation |

---

# 53. Glossary

| Term | Definition |
|------|------------|
| Aggregate | Cluster of related domain objects with a single root. |
| Bounded Context | Logical boundary containing a coherent domain model. |
| Clean Architecture | Architectural style separating business rules from infrastructure. |
| Correlation ID | Identifier used to trace a request end-to-end. |
| DDD | Domain-Driven Design. |
| Flyway | Database schema migration tool. |
| Idempotency | Multiple identical requests produce the same result. |
| JWT | JSON Web Token. |
| Modular Monolith | Single deployable application with strongly separated modules. |
| OpenAPI | Standard specification describing REST APIs. |
| RFC 9457 | Standard for HTTP Problem Details error responses. |
| Stateless | No conversational session stored inside application instances. |
| Value Object | Immutable domain object defined by its value rather than identity. |

---

# 54. References

## Standards

- RFC 9110 – HTTP Semantics
- RFC 9457 – Problem Details for HTTP APIs
- ISO 4217 – Currency Codes
- ISO 8601 – Date and Time Representation

## Framework Documentation

- Spring Boot Reference Documentation
- Spring Security Reference Documentation
- Spring Data JPA Reference Documentation
- Hibernate ORM Documentation
- PostgreSQL Documentation
- Flyway Documentation
- Docker Documentation
- OpenAPI Specification

## Architectural References

- Eric Evans – *Domain-Driven Design*
- Martin Fowler – *Patterns of Enterprise Application Architecture*
- Robert C. Martin – *Clean Architecture*
- Vaughn Vernon – *Implementing Domain-Driven Design*

---

# 55. Appendices

## Appendix A – Package Structure

```text
com.example.transactionaggregation
 ├── api
 ├── application
 ├── domain
 ├── infrastructure
 ├── configuration
 ├── shared
 └── bootstrap
```

## Appendix B – Module Summary

| Module | Responsibility |
|--------|----------------|
| Transaction | Ingestion and persistence |
| Customer | Customer management |
| Merchant | Merchant normalisation |
| Categorisation | Category assignment |
| Aggregation | Financial summaries |
| Audit | Audit events |
| Shared | Common abstractions |

## Appendix C – Quality Checklist

- Modular architecture
- Domain-driven design
- Clean Architecture
- Versioned REST API
- JWT security
- RFC 9457 error handling
- Structured logging
- Metrics and health checks
- Flyway migrations
- Automated testing
- Docker deployment
- OpenAPI documentation

## Appendix D – Recommended Repository Structure

```text
transaction-aggregation-api
├── src
├── docker
├── docs
├── scripts
├── .github
├── pom.xml
├── Dockerfile
├── compose.yaml
└── README.md
```

---

# 56. Governance Summary

The governance model provides a long-term framework for maintaining architectural consistency, operational excellence and controlled evolution. Architecture decisions are documented through ADRs, risks are actively managed, technology choices are standardised and supporting references ensure future maintainers understand the rationale behind the solution.

---

## End of Part 6B

With Parts 1 through 6B complete, the Solution Architecture Document Version 3 is ready to be merged into a single enterprise-grade document with a unified table of contents, continuous numbering, cross-references and consistent formatting.
