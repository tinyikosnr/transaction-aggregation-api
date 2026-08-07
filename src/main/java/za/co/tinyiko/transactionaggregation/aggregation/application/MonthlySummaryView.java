package za.co.tinyiko.transactionaggregation.aggregation.application;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * One row of {@link GetMonthlySummaryUseCase}'s response (TDS 35: income, expenditure and net
 * cash flow grouped by month - both directions, unlike category/merchant summaries).
 */
public record MonthlySummaryView(YearMonth month, BigDecimal totalIncome, BigDecimal totalExpenditure, BigDecimal netCashFlow, String currency, long transactionCount) {
}
