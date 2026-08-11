package za.co.tinyiko.transactionaggregation.transaction.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import za.co.tinyiko.transactionaggregation.transaction.port.CategoryTotalsRow;
import za.co.tinyiko.transactionaggregation.transaction.port.CustomerTotalsRow;
import za.co.tinyiko.transactionaggregation.transaction.port.MerchantTotalsRow;

/**
 * Aggregate read queries backing {@link JpaTransactionSummaryRepositoryAdapter}. Deliberately a
 * separate Spring Data interface from {@code SpringDataTransactionRepository}, mirroring the
 * separate outbound ports: this one is read/aggregate-only, that one is write/duplicate-check.
 *
 * <p>{@code COALESCE(SUM(...), 0)} matters here: SQL's {@code SUM} over zero matching rows
 * returns {@code NULL}, not zero - without it, a customer with no CREDIT (or no DEBIT, or no
 * transactions at all) transactions in range would get a {@code NullPointerException} from the
 * JPQL constructor expression rather than a legitimate zero total. Verified empirically against
 * a real Testcontainers PostgreSQL instance with a genuinely empty result set, not assumed.
 *
 * <p>{@code monthlyTotals} uses a native query, not JPQL: JPQL constructor expressions require
 * an exact Java-constructor type match, and there is no portable JPQL month-truncation
 * expression that projects cleanly into {@code java.time.YearMonth}. The native query returns
 * raw rows (month start as a database timestamp, two sums, a count), which
 * {@link JpaTransactionSummaryRepositoryAdapter} converts to {@code YearMonth} in Java.
 *
 * <p>{@code date_trunc('month', occurred_at AT TIME ZONE 'UTC')}, not bare
 * {@code date_trunc('month', occurred_at)}: PostgreSQL's {@code date_trunc} on a
 * {@code timestamptz} column truncates in the current session's timezone, not UTC. Verified
 * empirically that this matters - without {@code AT TIME ZONE 'UTC'}, a transaction occurring
 * at midnight UTC on the 1st of a month was bucketed into the *previous* month, because the
 * test session's timezone put that same instant on the last day of the prior month locally.
 * {@code AT TIME ZONE 'UTC'} converts the {@code timestamptz} to a plain UTC-wall-clock
 * timestamp before truncating, consistent with treating all timestamps internally as UTC.
 */
interface SpringDataTransactionSummaryRepository extends JpaRepository<TransactionEntity, UUID> {

	@Query("""
			SELECT new za.co.tinyiko.transactionaggregation.transaction.port.CustomerTotalsRow(
				COALESCE(SUM(CASE WHEN t.direction = 'CREDIT' THEN t.amount ELSE 0 END), 0),
				COALESCE(SUM(CASE WHEN t.direction = 'DEBIT' THEN t.amount ELSE 0 END), 0),
				COUNT(t))
			FROM TransactionEntity t
			WHERE t.customerId = :customerId AND t.status = 'PROCESSED'
				AND t.occurredAt >= :from AND t.occurredAt < :to
			""")
	CustomerTotalsRow customerTotals(@Param("customerId") UUID customerId, @Param("from") Instant from, @Param("to") Instant to);

	@Query("""
			SELECT new za.co.tinyiko.transactionaggregation.transaction.port.CategoryTotalsRow(
				t.categoryId, COALESCE(SUM(t.amount), 0), COUNT(t))
			FROM TransactionEntity t
			WHERE t.customerId = :customerId AND t.status = 'PROCESSED' AND t.direction = 'DEBIT'
				AND t.occurredAt >= :from AND t.occurredAt < :to
			GROUP BY t.categoryId
			""")
	List<CategoryTotalsRow> categoryTotals(@Param("customerId") UUID customerId, @Param("from") Instant from, @Param("to") Instant to);

	@Query("""
			SELECT new za.co.tinyiko.transactionaggregation.transaction.port.MerchantTotalsRow(
				t.merchantId, COALESCE(SUM(t.amount), 0), COUNT(t))
			FROM TransactionEntity t
			WHERE t.customerId = :customerId AND t.status = 'PROCESSED' AND t.direction = 'DEBIT'
				AND t.merchantId IS NOT NULL
				AND t.occurredAt >= :from AND t.occurredAt < :to
			GROUP BY t.merchantId
			""")
	List<MerchantTotalsRow> merchantTotals(@Param("customerId") UUID customerId, @Param("from") Instant from, @Param("to") Instant to);

	@Query(value = """
			SELECT date_trunc('month', occurred_at AT TIME ZONE 'UTC') AS month_start,
				COALESCE(SUM(CASE WHEN direction = 'CREDIT' THEN amount ELSE 0 END), 0) AS total_income,
				COALESCE(SUM(CASE WHEN direction = 'DEBIT' THEN amount ELSE 0 END), 0) AS total_expenditure,
				COUNT(*) AS transaction_count
			FROM transactions
			WHERE customer_id = :customerId AND status = 'PROCESSED'
				AND occurred_at >= :from AND occurred_at < :to
			GROUP BY month_start
			ORDER BY month_start
			""", nativeQuery = true)
	List<Object[]> monthlyTotalsRaw(@Param("customerId") UUID customerId, @Param("from") Instant from, @Param("to") Instant to);

}
