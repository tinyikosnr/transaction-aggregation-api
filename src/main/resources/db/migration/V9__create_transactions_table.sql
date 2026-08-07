CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    transaction_source_id UUID NOT NULL,
    external_transaction_id VARCHAR(150) NOT NULL,
    merchant_id UUID,
    category_id UUID NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency CHAR(3) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    description VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,

    CONSTRAINT fk_transactions_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_transactions_source FOREIGN KEY (transaction_source_id) REFERENCES transaction_sources (id),
    CONSTRAINT fk_transactions_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id),
    CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES transaction_categories (id),
    CONSTRAINT uk_transactions_source_external_id UNIQUE (transaction_source_id, external_transaction_id),
    CONSTRAINT ck_transactions_amount CHECK (amount > 0),
    CONSTRAINT ck_transactions_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_transactions_direction CHECK (direction IN ('CREDIT', 'DEBIT')),
    CONSTRAINT ck_transactions_status CHECK (status IN ('RECEIVED', 'PROCESSED', 'REJECTED'))
);

CREATE INDEX idx_transactions_customer_occurred_at ON transactions (customer_id, occurred_at);
CREATE INDEX idx_transactions_category ON transactions (category_id);
CREATE INDEX idx_transactions_merchant ON transactions (merchant_id);
CREATE INDEX idx_transactions_source ON transactions (transaction_source_id);
CREATE INDEX idx_transactions_status ON transactions (status);
