package za.co.tinyiko.transactionaggregation.api.mapper;

import java.util.List;

import za.co.tinyiko.transactionaggregation.aggregation.application.CategorySummaryView;
import za.co.tinyiko.transactionaggregation.aggregation.application.CustomerSummaryView;
import za.co.tinyiko.transactionaggregation.aggregation.application.MerchantSummaryView;
import za.co.tinyiko.transactionaggregation.aggregation.application.MonthlySummaryView;
import za.co.tinyiko.transactionaggregation.api.dto.response.CategorySummaryResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.CustomerSummaryResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.MerchantSummaryResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.MonthlySummaryResponse;

/**
 * Pure structural mapping between {@code api.dto} shapes and {@code aggregation.application}
 * contracts. Raw {@code from}/{@code to} query parameters are passed straight into the
 * {@code Get*SummaryUseCase}s, unchanged - {@code aggregation.domain.DateRange} is not exposed
 * cross-module, so construction and validation of it happens inside
 * {@code aggregation.application}, not here (see {@code GetCustomerSummaryUseCase}'s Javadoc).
 */
public final class AggregationApiMapper {

	private AggregationApiMapper() {
	}

	public static CustomerSummaryResponse toResponse(CustomerSummaryView view) {
		return new CustomerSummaryResponse(
				view.customerId(),
				new CustomerSummaryResponse.Period(view.periodFrom(), view.periodTo()),
				new CustomerSummaryResponse.MoneyAmount(view.totalIncome(), view.currency()),
				new CustomerSummaryResponse.MoneyAmount(view.totalExpenditure(), view.currency()),
				new CustomerSummaryResponse.MoneyAmount(view.netCashFlow(), view.currency()),
				view.transactionCount());
	}

	public static List<CategorySummaryResponse> toCategoryResponses(List<CategorySummaryView> views) {
		return views.stream()
				.map(view -> new CategorySummaryResponse(view.categoryId(), view.totalAmount(), view.currency(), view.transactionCount()))
				.toList();
	}

	public static List<MerchantSummaryResponse> toMerchantResponses(List<MerchantSummaryView> views) {
		return views.stream()
				.map(view -> new MerchantSummaryResponse(view.merchantId(), view.totalAmount(), view.currency(), view.transactionCount()))
				.toList();
	}

	public static List<MonthlySummaryResponse> toMonthlyResponses(List<MonthlySummaryView> views) {
		return views.stream()
				.map(view -> new MonthlySummaryResponse(view.month(), view.totalIncome(), view.totalExpenditure(), view.netCashFlow(), view.currency(), view.transactionCount()))
				.toList();
	}

}
