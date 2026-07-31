-- TDS 13's "Initial Categories" list. UNCATEGORISED is the sole is_fallback = TRUE row,
-- satisfying SAD 27.5's "at most one fallback category" invariant (enforced by V3's partial
-- unique index). RESTAURANTS and OTHER_EXPENSE have no matching rule in TDS 37's matrix yet,
-- but are seeded regardless since TDS 13 lists them as initial categories independent of
-- whether a rule currently targets them.
INSERT INTO transaction_categories (id, code, name, description, is_fallback, active, created_at, updated_at, version)
VALUES
    (gen_random_uuid(), 'SALARY', 'Salary', NULL, FALSE, TRUE, now(), now(), 0),
    (gen_random_uuid(), 'GROCERIES', 'Groceries', NULL, FALSE, TRUE, now(), now(), 0),
    (gen_random_uuid(), 'FUEL', 'Fuel', NULL, FALSE, TRUE, now(), now(), 0),
    (gen_random_uuid(), 'RESTAURANTS', 'Restaurants', NULL, FALSE, TRUE, now(), now(), 0),
    (gen_random_uuid(), 'TRANSPORT', 'Transport', NULL, FALSE, TRUE, now(), now(), 0),
    (gen_random_uuid(), 'UTILITIES', 'Utilities', NULL, FALSE, TRUE, now(), now(), 0),
    (gen_random_uuid(), 'ENTERTAINMENT', 'Entertainment', NULL, FALSE, TRUE, now(), now(), 0),
    (gen_random_uuid(), 'HEALTHCARE', 'Healthcare', NULL, FALSE, TRUE, now(), now(), 0),
    (gen_random_uuid(), 'INSURANCE', 'Insurance', NULL, FALSE, TRUE, now(), now(), 0),
    (gen_random_uuid(), 'BANK_FEES', 'Bank Fees', NULL, FALSE, TRUE, now(), now(), 0),
    (gen_random_uuid(), 'TRANSFER', 'Transfer', NULL, FALSE, TRUE, now(), now(), 0),
    (gen_random_uuid(), 'OTHER_INCOME', 'Other Income', NULL, FALSE, TRUE, now(), now(), 0),
    (gen_random_uuid(), 'OTHER_EXPENSE', 'Other Expense', NULL, FALSE, TRUE, now(), now(), 0),
    (gen_random_uuid(), 'UNCATEGORISED', 'Uncategorised', NULL, TRUE, TRUE, now(), now(), 0);
