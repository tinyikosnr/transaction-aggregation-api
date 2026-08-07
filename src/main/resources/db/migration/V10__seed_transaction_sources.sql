-- TDS 11's "Initial Sources" list.
INSERT INTO transaction_sources (id, code, name, status, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'MOCK_BANK_A', 'Mock Bank A', 'ACTIVE', now(), now()),
    (gen_random_uuid(), 'MOCK_BANK_B', 'Mock Bank B', 'ACTIVE', now(), now()),
    (gen_random_uuid(), 'MANUAL_UPLOAD', 'Manual Upload', 'ACTIVE', now(), now());
