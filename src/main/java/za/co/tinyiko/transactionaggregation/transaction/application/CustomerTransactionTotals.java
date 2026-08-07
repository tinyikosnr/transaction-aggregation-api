package za.co.tinyiko.transactionaggregation.transaction.application;

import java.math.BigDecimal;

/**
 * The outcome of {@link TransactionQueryPort#customerTotals}. Primitives only, exposed
 * cross-module via this package's {@code @NamedInterface} - deliberately a distinct type from
 * {@code transaction.port.CustomerTotalsRow}, which stays internal to the port/persistence
 * layer (see that type's Javadoc for why).
 */
public record CustomerTransactionTotals(BigDecimal totalIncome, BigDecimal totalExpenditure, long transactionCount) {
}
