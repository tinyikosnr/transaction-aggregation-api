-- feature/transaction-query: transaction search (TDS 31) commonly combines a category or
-- merchant filter with an occurred_at date range - the single-column indexes V9 created for
-- category_id/merchant_id serve the filter alone well, but force a second pass to apply the date
-- range on top. Composite indexes serve both in one index scan.
--
-- Replacing, not adding alongside: a composite index on (category_id, occurred_at) already
-- serves category-only queries via leading-column usage, so keeping the single-column index too
-- would be pure write-overhead with no read benefit.
DROP INDEX idx_transactions_category;
DROP INDEX idx_transactions_merchant;

CREATE INDEX idx_transactions_category_occurred_at
    ON transactions (category_id, occurred_at);

CREATE INDEX idx_transactions_merchant_occurred_at
    ON transactions (merchant_id, occurred_at);
