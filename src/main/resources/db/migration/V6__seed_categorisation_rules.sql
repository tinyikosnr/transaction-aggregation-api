-- TDS 37's Initial Categorisation Matrix, expanded one row per keyword: CategorisationRule
-- (domain model, and this table) holds a single match_value per rule, not a keyword list, so a
-- matrix row listing several keywords becomes several rules sharing that row's priority.
-- Sharing a priority across rules is safe: they all resolve to the same category regardless of
-- which one matches first, so evaluation order among them is immaterial.
--
-- operator is CONTAINS throughout, per TDS 14 ("Initial implementation uses CONTAINS").
-- category_id is resolved by code via subquery rather than a hardcoded UUID, since V5 assigns
-- category ids with gen_random_uuid() rather than fixed literals.
--
-- Priority 999/1000 in TDS 37 are documented as "Any match field, no match, category". Rather
-- than a literal any-field/no-keyword mechanism (not something MatchField/MatchOperator can
-- express), these are represented as REGEX '.*' rules against DESCRIPTION - description is
-- always present on a transaction (unlike merchant text), so a description-targeted catch-all
-- is guaranteed to fire when nothing more specific has already matched by that priority.
INSERT INTO categorisation_rules (id, category_id, match_field, operator, match_value, direction, priority, active, created_at, updated_at, version)
VALUES
    -- 10 | Description | SALARY, PAYROLL, WAGES | CREDIT | SALARY
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'SALARY'), 'DESCRIPTION', 'CONTAINS', 'SALARY', 'CREDIT', 10, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'SALARY'), 'DESCRIPTION', 'CONTAINS', 'PAYROLL', 'CREDIT', 10, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'SALARY'), 'DESCRIPTION', 'CONTAINS', 'WAGES', 'CREDIT', 10, TRUE, now(), now(), 0),

    -- 20 | Merchant | CHECKERS, PICK N PAY, SHOPRITE, SPAR, WOOLWORTHS FOOD | DEBIT | GROCERIES
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'GROCERIES'), 'MERCHANT', 'CONTAINS', 'CHECKERS', 'DEBIT', 20, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'GROCERIES'), 'MERCHANT', 'CONTAINS', 'PICK N PAY', 'DEBIT', 20, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'GROCERIES'), 'MERCHANT', 'CONTAINS', 'SHOPRITE', 'DEBIT', 20, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'GROCERIES'), 'MERCHANT', 'CONTAINS', 'SPAR', 'DEBIT', 20, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'GROCERIES'), 'MERCHANT', 'CONTAINS', 'WOOLWORTHS FOOD', 'DEBIT', 20, TRUE, now(), now(), 0),

    -- 30 | Merchant | SHELL, BP, ENGEN, SASOL | DEBIT | FUEL
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'FUEL'), 'MERCHANT', 'CONTAINS', 'SHELL', 'DEBIT', 30, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'FUEL'), 'MERCHANT', 'CONTAINS', 'BP', 'DEBIT', 30, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'FUEL'), 'MERCHANT', 'CONTAINS', 'ENGEN', 'DEBIT', 30, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'FUEL'), 'MERCHANT', 'CONTAINS', 'SASOL', 'DEBIT', 30, TRUE, now(), now(), 0),

    -- 40 | Merchant | UBER, BOLT, GAUTRAIN | DEBIT | TRANSPORT
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'TRANSPORT'), 'MERCHANT', 'CONTAINS', 'UBER', 'DEBIT', 40, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'TRANSPORT'), 'MERCHANT', 'CONTAINS', 'BOLT', 'DEBIT', 40, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'TRANSPORT'), 'MERCHANT', 'CONTAINS', 'GAUTRAIN', 'DEBIT', 40, TRUE, now(), now(), 0),

    -- 50 | Merchant | ESKOM, CITY POWER, MUNICIPALITY, TELKOM, VODACOM, MTN | DEBIT | UTILITIES
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'UTILITIES'), 'MERCHANT', 'CONTAINS', 'ESKOM', 'DEBIT', 50, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'UTILITIES'), 'MERCHANT', 'CONTAINS', 'CITY POWER', 'DEBIT', 50, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'UTILITIES'), 'MERCHANT', 'CONTAINS', 'MUNICIPALITY', 'DEBIT', 50, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'UTILITIES'), 'MERCHANT', 'CONTAINS', 'TELKOM', 'DEBIT', 50, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'UTILITIES'), 'MERCHANT', 'CONTAINS', 'VODACOM', 'DEBIT', 50, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'UTILITIES'), 'MERCHANT', 'CONTAINS', 'MTN', 'DEBIT', 50, TRUE, now(), now(), 0),

    -- 60 | Merchant | NETFLIX, SHOWMAX, SPOTIFY, DSTV | DEBIT | ENTERTAINMENT
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'ENTERTAINMENT'), 'MERCHANT', 'CONTAINS', 'NETFLIX', 'DEBIT', 60, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'ENTERTAINMENT'), 'MERCHANT', 'CONTAINS', 'SHOWMAX', 'DEBIT', 60, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'ENTERTAINMENT'), 'MERCHANT', 'CONTAINS', 'SPOTIFY', 'DEBIT', 60, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'ENTERTAINMENT'), 'MERCHANT', 'CONTAINS', 'DSTV', 'DEBIT', 60, TRUE, now(), now(), 0),

    -- 70 | Merchant | DIS-CHEM, CLICKS, MEDICLINIC, NETCARE | DEBIT | HEALTHCARE
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'HEALTHCARE'), 'MERCHANT', 'CONTAINS', 'DIS-CHEM', 'DEBIT', 70, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'HEALTHCARE'), 'MERCHANT', 'CONTAINS', 'CLICKS', 'DEBIT', 70, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'HEALTHCARE'), 'MERCHANT', 'CONTAINS', 'MEDICLINIC', 'DEBIT', 70, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'HEALTHCARE'), 'MERCHANT', 'CONTAINS', 'NETCARE', 'DEBIT', 70, TRUE, now(), now(), 0),

    -- 80 | Description | INSURANCE, PREMIUM | DEBIT | INSURANCE
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'INSURANCE'), 'DESCRIPTION', 'CONTAINS', 'INSURANCE', 'DEBIT', 80, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'INSURANCE'), 'DESCRIPTION', 'CONTAINS', 'PREMIUM', 'DEBIT', 80, TRUE, now(), now(), 0),

    -- 90 | Description | BANK FEE, SERVICE FEE, MONTHLY FEE | DEBIT | BANK_FEES
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'BANK_FEES'), 'DESCRIPTION', 'CONTAINS', 'BANK FEE', 'DEBIT', 90, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'BANK_FEES'), 'DESCRIPTION', 'CONTAINS', 'SERVICE FEE', 'DEBIT', 90, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'BANK_FEES'), 'DESCRIPTION', 'CONTAINS', 'MONTHLY FEE', 'DEBIT', 90, TRUE, now(), now(), 0),

    -- 100 | Description | TRANSFER, PAYMENT RECEIVED | CREDIT | OTHER_INCOME
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'OTHER_INCOME'), 'DESCRIPTION', 'CONTAINS', 'TRANSFER', 'CREDIT', 100, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'OTHER_INCOME'), 'DESCRIPTION', 'CONTAINS', 'PAYMENT RECEIVED', 'CREDIT', 100, TRUE, now(), now(), 0),

    -- 110 | Description | TRANSFER, EFT | DEBIT | TRANSFER
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'TRANSFER'), 'DESCRIPTION', 'CONTAINS', 'TRANSFER', 'DEBIT', 110, TRUE, now(), now(), 0),
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'TRANSFER'), 'DESCRIPTION', 'CONTAINS', 'EFT', 'DEBIT', 110, TRUE, now(), now(), 0),

    -- 999 | Any | No match | CREDIT | OTHER_INCOME  (see file header re: REGEX '.*' representation)
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'OTHER_INCOME'), 'DESCRIPTION', 'REGEX', '.*', 'CREDIT', 999, TRUE, now(), now(), 0),

    -- 1000 | Any | No match | DEBIT | UNCATEGORISED  (see file header re: REGEX '.*' representation)
    (gen_random_uuid(), (SELECT id FROM transaction_categories WHERE code = 'UNCATEGORISED'), 'DESCRIPTION', 'REGEX', '.*', 'DEBIT', 1000, TRUE, now(), now(), 0);
