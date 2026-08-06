CREATE TABLE transaction_categories (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    is_fallback BOOLEAN NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,

    CONSTRAINT uk_transaction_categories_code UNIQUE (code)
);

-- Enforces "at most one fallback category" (SAD 27.5) without an application-level check:
-- a partial unique index only considers rows where is_fallback is TRUE, so any number of
-- FALSE rows are unrestricted while a second TRUE row is rejected by the database itself.
CREATE UNIQUE INDEX uk_transaction_categories_single_fallback
    ON transaction_categories (is_fallback)
    WHERE is_fallback = TRUE;
