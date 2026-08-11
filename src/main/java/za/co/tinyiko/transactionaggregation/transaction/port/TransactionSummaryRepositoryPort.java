package za.co.tinyiko.transactionaggregation.transaction.port;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Outbound port {@code TransactionQueryService} (in {@code transaction.application}) uses to
 * read pre-aggregated transaction totals - a separate port from {@link TransactionRepositoryPort},
 * not an addition to it: that port is write/duplicate-check-shaped with an already-focused,
 * tested purpose, and bolting four read-aggregate methods onto it would blur that, the same
 * reasoning {@code categorisation} already applies by keeping {@code CategoryRepositoryPort}/
 * {@code CategorisationRuleRepositoryPort} separate.
 *
 * <p>Bounds are exclusive-upper {@code Instant} ranges (not {@code LocalDate}): translating a
 * date-range into UTC instant bounds is {@code TransactionQueryService}'s job, not this port's -
 * keeping the port itself a plain "given these exact instants, aggregate these rows" contract.
 *
 * <p>All aggregation happens in SQL (via the adapter's JPQL queries), not by loading transactions
 * into memory.
 */
public interface TransactionSummaryRepositoryPort {

	CustomerTotalsRow customerTotals(UUID customerId, Instant fromInclusive, Instant toExclusive);

	List<CategoryTotalsRow> categoryTotals(UUID customerId, Instant fromInclusive, Instant toExclusive);

	List<MerchantTotalsRow> merchantTotals(UUID customerId, Instant fromInclusive, Instant toExclusive);

	List<MonthlyTotalsRow> monthlyTotals(UUID customerId, Instant fromInclusive, Instant toExclusive);

}
