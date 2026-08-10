package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The customer financial summary response (TDS 32). Reshapes
 * {@code aggregation.application.CustomerSummaryView}'s single shared {@code currency} field
 * into a nested amount+currency object per money field, matching TDS 32's documented JSON.
 */
public record CustomerSummaryResponse(
		@Schema(description = "Identifier of the summarised customer.") UUID customerId,
		@Schema(description = "Date range this summary covers (both bounds inclusive, max 24-month span).") Period period,
		@Schema(description = "Total of all CREDIT transactions in the range.") MoneyAmount totalIncome,
		@Schema(description = "Total of all DEBIT transactions in the range.") MoneyAmount totalExpenditure,
		@Schema(description = "totalIncome minus totalExpenditure.") MoneyAmount netCashFlow,
		@Schema(description = "Number of transactions included in this summary.") long transactionCount
) {

	public record Period(
			@Schema(description = "Inclusive start date.") LocalDate from,
			@Schema(description = "Inclusive end date.") LocalDate to
	) {
	}

	public record MoneyAmount(
			@Schema(description = "Monetary amount.") BigDecimal amount,
			@Schema(description = "Three-letter uppercase ISO 4217 currency code.", example = "ZAR") String currency
	) {
	}

}
