package za.co.tinyiko.transactionaggregation.transaction.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Inbound port other modules use to read pre-aggregated transaction totals for reporting (TDS
 * 6, 60). {@code aggregation} is this port's first and, in this branch, only caller.
 *
 * <p>Takes {@code LocalDate} bounds, not {@code Instant}: this is the date-range shape TDS 20's
 * {@code DateRange} and the summary API contracts (TDS 32-35) use. Converting to the UTC instant
 * range {@code transaction.port.TransactionSummaryRepositoryPort} actually queries against is
 * this port's implementation's job ({@code TransactionQueryService}), not the caller's.
 */
public interface TransactionQueryPort {

	CustomerTransactionTotals customerTotals(UUID customerId, LocalDate from, LocalDate to);

	List<CategoryTransactionTotal> categoryTotals(UUID customerId, LocalDate from, LocalDate to);

	List<MerchantTransactionTotal> merchantTotals(UUID customerId, LocalDate from, LocalDate to);

	List<MonthlyTransactionTotal> monthlyTotals(UUID customerId, LocalDate from, LocalDate to);

}
