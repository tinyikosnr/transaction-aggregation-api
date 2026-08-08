package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The customer financial summary response (TDS 32). Reshapes
 * {@code aggregation.application.CustomerSummaryView}'s single shared {@code currency} field
 * into a nested amount+currency object per money field, matching TDS 32's documented JSON.
 */
public record CustomerSummaryResponse(
		UUID customerId,
		Period period,
		MoneyAmount totalIncome,
		MoneyAmount totalExpenditure,
		MoneyAmount netCashFlow,
		long transactionCount
) {

	public record Period(LocalDate from, LocalDate to) {
	}

	public record MoneyAmount(BigDecimal amount, String currency) {
	}

}
