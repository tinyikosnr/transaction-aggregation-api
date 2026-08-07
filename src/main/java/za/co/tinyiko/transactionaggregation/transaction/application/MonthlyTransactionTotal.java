package za.co.tinyiko.transactionaggregation.transaction.application;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * One grouped row of {@link TransactionQueryPort#monthlyTotals} (both directions, per TDS 35).
 * See {@code CustomerTransactionTotals}'s Javadoc for why this is a distinct type from
 * {@code transaction.port.MonthlyTotalsRow}.
 */
public record MonthlyTransactionTotal(YearMonth month, BigDecimal totalIncome, BigDecimal totalExpenditure, long transactionCount) {
}
