# Transaction Aggregation API
# Solution Architecture Document (SAD)

**Version:** 2.0  
**Author:** Tinyiko Chauke  
**Date:** July 2026  

> This version replaces the previous text-based diagrams with professional Mermaid diagrams that can be rendered by GitHub, GitLab, VS Code, Obsidian and many Markdown viewers.

---

# Table of Contents

1. Introduction
2. Business Context
3. Requirements
4. High-Level Architecture
5. Module Design
6. Domain Model
7. Database Design
8. API Design
9. Security
10. Deployment
11. Testing
12. Scalability
13. Future Evolution
14. ADR Summary
15. Risks
16. Future Improvements

---

# Architecture Diagrams

## 1. C4 Context Diagram

```mermaid
flowchart LR
    User["Client Applications"]
    BankA["Mock Bank A"]
    BankB["Mock Bank B"]
    API["Transaction Aggregation API"]
    DB[(PostgreSQL)]
    Monitor["Monitoring / Actuator"]

    User --> API
    BankA --> API
    BankB --> API
    API --> DB
    API --> Monitor
```

---

## 2. C4 Container Diagram

```mermaid
flowchart TB
    Client["REST Clients"]
    Rest["Spring Boot REST API"]
    App["Application Layer"]
    Domain["Domain Layer"]
    Persistence["Persistence Layer"]
    DB[(PostgreSQL)]

    Client --> Rest
    Rest --> App
    App --> Domain
    Domain --> Persistence
    Persistence --> DB
```

---

## 3. Module Dependency Diagram

```mermaid
flowchart LR
    Ingestion --> Transaction
    Transaction --> Categorisation
    Transaction --> Customer
    Transaction --> Merchant
    Transaction --> Audit
    Aggregation --> Transaction
    Aggregation --> Customer
    Audit --> Shared
    Transaction --> Shared
    Categorisation --> Shared
```

---

## 4. Component Diagram

```mermaid
flowchart TB
    Controller["TransactionController"]
    Service["CreateTransactionService"]
    Validator["TransactionValidator"]
    Duplicate["DuplicateChecker"]
    Categoriser["CategorisationService"]
    Repository["TransactionRepository"]
    Events["Domain Event Publisher"]
    DB[(PostgreSQL)]

    Controller --> Service
    Service --> Validator
    Service --> Duplicate
    Service --> Categoriser
    Service --> Repository
    Repository --> DB
    Service --> Events
```

---

## 5. Crow's Foot Style ER Overview

```mermaid
erDiagram
    CUSTOMER ||--o{ TRANSACTION : owns
    TRANSACTION_SOURCE ||--o{ TRANSACTION : supplies
    MERCHANT ||--o{ TRANSACTION : receives
    TRANSACTION_CATEGORY ||--o{ TRANSACTION : classifies
    TRANSACTION ||--o{ AUDIT_EVENT : generates
```

---

## 6. Sequence Diagram – Create Transaction

```mermaid
sequenceDiagram
    participant Client
    participant Controller
    participant Service
    participant Validator
    participant Categoriser
    participant Repository
    participant DB

    Client->>Controller: POST /transactions
    Controller->>Service: Create request
    Service->>Validator: Validate
    Validator-->>Service: OK
    Service->>Categoriser: Determine category
    Categoriser-->>Service: Category
    Service->>Repository: Save transaction
    Repository->>DB: INSERT
    DB-->>Repository: Success
    Repository-->>Service: Transaction
    Service-->>Controller: Response
    Controller-->>Client: 201 Created
```

---

## 7. Sequence Diagram – Customer Summary

```mermaid
sequenceDiagram
    participant Client
    participant Controller
    participant AggregationService
    participant Repository
    participant DB

    Client->>Controller: GET /customers/{id}/summary
    Controller->>AggregationService: Request summary
    AggregationService->>Repository: Query transactions
    Repository->>DB: SELECT
    DB-->>Repository: Results
    Repository-->>AggregationService: Transactions
    AggregationService-->>Controller: Summary
    Controller-->>Client: 200 OK
```

---

## 8. Deployment Diagram

```mermaid
flowchart LR
    Client --> LB["Load Balancer / Reverse Proxy"]
    LB --> App["Spring Boot Container"]
    App --> DB[(PostgreSQL)]
```

---

## 9. Future Evolution Diagram

```mermaid
flowchart TB
    Monolith["Today's Modular Monolith"]
    Monolith --> TransactionSvc["Transaction Service"]
    Monolith --> CategorisationSvc["Categorisation Service"]
    Monolith --> AggregationSvc["Aggregation Service"]
    Monolith --> AuditSvc["Audit Service"]

    TransactionSvc -.Events / REST.-> CategorisationSvc
    CategorisationSvc -.Events / REST.-> AggregationSvc
    AggregationSvc -.Events / REST.-> AuditSvc
```

---

# Improvements over Version 1

- Replaced all ASCII/text diagrams with Mermaid diagrams.
- Standardised diagram style across the document.
- Added a formal table of contents.
- Ready for rendering on GitHub, GitLab and modern Markdown editors.
- Diagrams align with the approved modular monolith architecture.
- Prepared for future conversion into a PDF or Word document with embedded rendered diagrams.

---

# Remaining SAD Content

The functional requirements, non-functional requirements, business context, architecture decisions, domain model, database design, API design, security, deployment, testing, scalability, ADRs, risks and future improvements from Version 1 remain unchanged and should be appended after these updated sections (or retained from the merged SAD) as they are still valid.
