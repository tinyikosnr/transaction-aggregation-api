CREATE TABLE categorisation_rules (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL,
    match_field VARCHAR(20) NOT NULL,
    operator VARCHAR(20) NOT NULL,
    match_value VARCHAR(200) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    priority INT NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,

    CONSTRAINT fk_categorisation_rules_category FOREIGN KEY (category_id) REFERENCES transaction_categories (id),
    CONSTRAINT ck_categorisation_rules_match_field CHECK (match_field IN ('MERCHANT', 'DESCRIPTION')),
    CONSTRAINT ck_categorisation_rules_operator CHECK (operator IN ('EQUALS', 'CONTAINS', 'REGEX')),
    CONSTRAINT ck_categorisation_rules_direction CHECK (direction IN ('CREDIT', 'DEBIT')),
    CONSTRAINT ck_categorisation_rules_priority CHECK (priority > 0)
);

-- Supports CategorisationRuleRepositoryPort#findAllActive, which loads active rules for the
-- engine to sort by priority (CategorisationRuleEngine sorts defensively in-memory regardless,
-- but an index matching the query's own filter/order keeps that load itself efficient).
CREATE INDEX ix_categorisation_rules_active_priority
    ON categorisation_rules (active, priority);
