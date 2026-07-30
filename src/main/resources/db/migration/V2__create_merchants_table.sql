CREATE TABLE merchants (
    id UUID PRIMARY KEY,
    normalised_name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,

    CONSTRAINT uk_merchants_normalised_name UNIQUE (normalised_name)
);
