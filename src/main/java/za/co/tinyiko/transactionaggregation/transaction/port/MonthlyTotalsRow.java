package za.co.tinyiko.transactionaggregation.transaction.port;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Internal query projection for {@link TransactionSummaryRepositoryPort#monthlyTotals}. See
 * {@link CustomerTotalsRow}'s Javadoc for why this stays in {@code transaction.port} rather
 * than reusing {@code transaction.application.MonthlyTransactionTotal}.
 */
public record MonthlyTotalsRow(YearMonth month, BigDecimal totalIncome, BigDecimal totalExpenditure, long transactionCount) {
}
