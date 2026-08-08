package za.co.tinyiko.transactionaggregation.api.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.aggregation.application.CategorySummaryView;
import za.co.tinyiko.transactionaggregation.aggregation.application.CustomerSummaryView;
import za.co.tinyiko.transactionaggregation.aggregation.application.MerchantSummaryView;
import za.co.tinyiko.transactionaggregation.aggregation.application.MonthlySummaryView;
import za.co.tinyiko.transactionaggregation.api.dto.response.CategorySummaryResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.CustomerSummaryResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.MerchantSummaryResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.MonthlySummaryResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AggregationApiMapperTests {

	@Test
	void reshapesTheSharedCurrencyFieldIntoAPerAmountMoneyObject() {
		UUID customerId = UUID.randomUUID();
		CustomerSummaryView view = new CustomerSummaryView(customerId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
				new BigDecimal("5000.00"), new BigDecimal("3212.50"), new BigDecimal("1787.50"), "ZAR", 82);

		CustomerSummaryResponse response = AggregationApiMapper.toResponse(view);

		assertThat(response.customerId()).isEqualTo(customerId);
		assertThat(response.period()).isEqualTo(new CustomerSummaryResponse.Period(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)));
		assertThat(response.totalIncome()).isEqualTo(new CustomerSummaryResponse.MoneyAmount(new BigDecimal("5000.00"), "ZAR"));
		assertThat(response.totalExpenditure()).isEqualTo(new CustomerSummaryResponse.MoneyAmount(new BigDecimal("3212.50"), "ZAR"));
		assertThat(response.netCashFlow()).isEqualTo(new CustomerSummaryResponse.MoneyAmount(new BigDecimal("1787.50"), "ZAR"));
		assertThat(response.transactionCount()).isEqualTo(82);
	}

	@Test
	void mapsEachCategorySummaryViewToAResponseRow() {
		UUID categoryId = UUID.randomUUID();
		List<CategorySummaryResponse> responses = AggregationApiMapper.toCategoryResponses(
				List.of(new CategorySummaryView(categoryId, new BigDecimal("130.00"), "ZAR", 2)));

		assertThat(responses).containsExactly(new CategorySummaryResponse(categoryId, new BigDecimal("130.00"), "ZAR", 2));
	}

	@Test
	void mapsEachMerchantSummaryViewToAResponseRow() {
		UUID merchantId = UUID.randomUUID();
		List<MerchantSummaryResponse> responses = AggregationApiMapper.toMerchantResponses(
				List.of(new MerchantSummaryView(merchantId, new BigDecimal("100.00"), "ZAR", 1)));

		assertThat(responses).containsExactly(new MerchantSummaryResponse(merchantId, new BigDecimal("100.00"), "ZAR", 1));
	}

	@Test
	void mapsEachMonthlySummaryViewToAResponseRow() {
		YearMonth month = YearMonth.of(2026, 1);
		List<MonthlySummaryResponse> responses = AggregationApiMapper.toMonthlyResponses(
				List.of(new MonthlySummaryView(month, new BigDecimal("5000.00"), new BigDecimal("100.00"), new BigDecimal("4900.00"), "ZAR", 2)));

		assertThat(responses).containsExactly(new MonthlySummaryResponse(month, new BigDecimal("5000.00"), new BigDecimal("100.00"), new BigDecimal("4900.00"), "ZAR", 2));
	}

}
