package za.co.tinyiko.transactionaggregation.aggregation.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The response shape of {@link GetCustomerSummaryUseCase}, matching TDS 32's documented JSON
 * contract. {@code currency} is a single field, not per-amount: the system is single-currency
 * (ZAR) in current practice (see {@code transaction.domain.Money}'s own documented scope), so
 * one field for the whole view is a faithful simplification of TDS 32's per-amount currency,
 * not a loss of information given every amount here shares the same currency.
 */
public record CustomerSummaryView(
		UUID customerId,
		LocalDate periodFrom,
		LocalDate periodTo,
		BigDecimal totalIncome,
		BigDecimal totalExpenditure,
		BigDecimal netCashFlow,
		String currency,
		long transactionCount
) {
}
