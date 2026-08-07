CREATE TABLE transaction_sources (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_transaction_sources_code UNIQUE (code),
    CONSTRAINT ck_transaction_sources_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
