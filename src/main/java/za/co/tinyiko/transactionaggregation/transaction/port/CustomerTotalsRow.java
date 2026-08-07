package za.co.tinyiko.transactionaggregation.transaction.port;

import java.math.BigDecimal;

/**
 * Internal query projection for {@link TransactionSummaryRepositoryPort#customerTotals}. Lives
 * in {@code transaction.port}, not {@code transaction.application}: the port layer must never
 * depend on the application layer (only the reverse - {@code application -> port ->
 * persistence}), so this is deliberately a separate, primitive-based type from the public
 * {@code transaction.application.CustomerTransactionTotals} it gets mapped to by
 * {@code TransactionQueryService}, even though the two shapes happen to look identical today.
 */
public record CustomerTotalsRow(BigDecimal totalIncome, BigDecimal totalExpenditure, long transactionCount) {
}
