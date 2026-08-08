package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * One row of the customer's monthly breakdown (TDS 35).
 */
public record MonthlySummaryResponse(YearMonth month, BigDecimal totalIncome, BigDecimal totalExpenditure, BigDecimal netCashFlow, String currency, long transactionCount) {
}
