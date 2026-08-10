package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.math.BigDecimal;
import java.time.YearMonth;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One row of the customer's monthly breakdown (TDS 35).
 */
@Schema(description = "Income/expenditure totals for one calendar month, bucketed in UTC.")
public record MonthlySummaryResponse(
		@Schema(description = "Calendar month (UTC), formatted as YYYY-MM.", example = "2026-03") YearMonth month,
		@Schema(description = "Total of all CREDIT transactions in this month.") BigDecimal totalIncome,
		@Schema(description = "Total of all DEBIT transactions in this month.") BigDecimal totalExpenditure,
		@Schema(description = "totalIncome minus totalExpenditure.") BigDecimal netCashFlow,
		@Schema(description = "Three-letter uppercase ISO 4217 currency code.", example = "ZAR") String currency,
		@Schema(description = "Number of transactions in this month.") long transactionCount
) {
}
