CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    actor VARCHAR(150) NOT NULL,
    correlation_id VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_events_correlation_id
    ON audit_events (correlation_id);

CREATE INDEX idx_audit_events_aggregate
    ON audit_events (aggregate_type, aggregate_id);

CREATE INDEX idx_audit_events_occurred_at
    ON audit_events (occurred_at);
