package za.co.tinyiko.transactionaggregation.api.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import za.co.tinyiko.transactionaggregation.aggregation.application.CategorySummaryView;
import za.co.tinyiko.transactionaggregation.aggregation.application.CustomerNotFoundException;
import za.co.tinyiko.transactionaggregation.aggregation.application.CustomerSummaryView;
import za.co.tinyiko.transactionaggregation.aggregation.application.GetCategorySummaryUseCase;
import za.co.tinyiko.transactionaggregation.aggregation.application.GetCustomerSummaryUseCase;
import za.co.tinyiko.transactionaggregation.aggregation.application.GetMerchantSummaryUseCase;
import za.co.tinyiko.transactionaggregation.aggregation.application.GetMonthlySummaryUseCase;
import za.co.tinyiko.transactionaggregation.aggregation.application.MerchantSummaryView;
import za.co.tinyiko.transactionaggregation.aggregation.application.MonthlySummaryView;
import za.co.tinyiko.transactionaggregation.security.SecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @WebMvcTest} slice - see {@code TransactionControllerTests}'s Javadoc for why
 * {@code SecurityConfig} is explicitly imported and {@code JwtDecoder} is mocked.
 */
@WebMvcTest(AggregationController.class)
@Import(SecurityConfig.class)
class AggregationControllerTests {

	private static final UUID CUSTOMER_ID = UUID.randomUUID();
	private static final String FROM = "2026-01-01";
	private static final String TO = "2026-01-31";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GetCustomerSummaryUseCase getCustomerSummaryUseCase;
	@MockitoBean
	private GetCategorySummaryUseCase getCategorySummaryUseCase;
	@MockitoBean
	private GetMerchantSummaryUseCase getMerchantSummaryUseCase;
	@MockitoBean
	private GetMonthlySummaryUseCase getMonthlySummaryUseCase;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	private static RequestPostProcessor aggregationReadToken() {
		return jwt().jwt(builder -> builder.subject("jwt-subject-001")).authorities(new SimpleGrantedAuthority("AGGREGATION_READ"));
	}

	@Test
	void returnsCustomerSummaryAsNestedMoneyAmounts() throws Exception {
		when(getCustomerSummaryUseCase.get(eq(CUSTOMER_ID), any(), any())).thenReturn(new CustomerSummaryView(
				CUSTOMER_ID, LocalDate.parse(FROM), LocalDate.parse(TO),
				new BigDecimal("5000.00"), new BigDecimal("3212.50"), new BigDecimal("1787.50"), "ZAR", 82));

		mockMvc.perform(get("/api/v1/customers/{customerId}/summary", CUSTOMER_ID)
						.with(aggregationReadToken()).param("from", FROM).param("to", TO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.customerId").value(CUSTOMER_ID.toString()))
				.andExpect(jsonPath("$.totalIncome.amount").value(5000.00))
				.andExpect(jsonPath("$.totalIncome.currency").value("ZAR"))
				.andExpect(jsonPath("$.transactionCount").value(82));
	}

	@Test
	void returnsCategorySummaryAsAJsonArray() throws Exception {
		UUID categoryId = UUID.randomUUID();
		when(getCategorySummaryUseCase.get(eq(CUSTOMER_ID), any(), any()))
				.thenReturn(List.of(new CategorySummaryView(categoryId, new BigDecimal("130.00"), "ZAR", 2)));

		mockMvc.perform(get("/api/v1/customers/{customerId}/categories", CUSTOMER_ID)
						.with(aggregationReadToken()).param("from", FROM).param("to", TO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].categoryId").value(categoryId.toString()))
				.andExpect(jsonPath("$[0].totalAmount").value(130.00));
	}

	@Test
	void returnsMerchantSummaryAsAJsonArray() throws Exception {
		UUID merchantId = UUID.randomUUID();
		when(getMerchantSummaryUseCase.get(eq(CUSTOMER_ID), any(), any()))
				.thenReturn(List.of(new MerchantSummaryView(merchantId, new BigDecimal("100.00"), "ZAR", 1)));

		mockMvc.perform(get("/api/v1/customers/{customerId}/merchants", CUSTOMER_ID)
						.with(aggregationReadToken()).param("from", FROM).param("to", TO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].merchantId").value(merchantId.toString()));
	}

	@Test
	void returnsMonthlySummaryAsAJsonArray() throws Exception {
		when(getMonthlySummaryUseCase.get(eq(CUSTOMER_ID), any(), any())).thenReturn(List.of(
				new MonthlySummaryView(YearMonth.of(2026, 1), new BigDecimal("5000.00"), new BigDecimal("100.00"), new BigDecimal("4900.00"), "ZAR", 2)));

		mockMvc.perform(get("/api/v1/customers/{customerId}/monthly-summary", CUSTOMER_ID)
						.with(aggregationReadToken()).param("from", FROM).param("to", TO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].month").value("2026-01"));
	}

	@Test
	void returnsBadRequestWhenFromParameterIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/customers/{customerId}/summary", CUSTOMER_ID).with(aggregationReadToken()).param("to", TO))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("REQUEST_VALIDATION_FAILED"));
	}

	@Test
	void returnsBadRequestWhenDateIsMalformed() throws Exception {
		mockMvc.perform(get("/api/v1/customers/{customerId}/summary", CUSTOMER_ID)
						.with(aggregationReadToken()).param("from", "not-a-date").param("to", TO))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("REQUEST_VALIDATION_FAILED"));
	}

	@Test
	void returnsBadRequestWithInvalidDateRangeWhenTheUseCaseRejectsAnInvalidRange() throws Exception {
		when(getCustomerSummaryUseCase.get(eq(CUSTOMER_ID), any(), any()))
				.thenThrow(new IllegalArgumentException("from must not be after to"));

		mockMvc.perform(get("/api/v1/customers/{customerId}/summary", CUSTOMER_ID)
						.with(aggregationReadToken()).param("from", TO).param("to", FROM))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("INVALID_DATE_RANGE"));
	}

	@Test
	void returnsNotFoundWithCustomerNotFoundWhenTheCustomerDoesNotExist() throws Exception {
		when(getCustomerSummaryUseCase.get(eq(CUSTOMER_ID), any(), any())).thenThrow(new CustomerNotFoundException(CUSTOMER_ID));

		mockMvc.perform(get("/api/v1/customers/{customerId}/summary", CUSTOMER_ID)
						.with(aggregationReadToken()).param("from", FROM).param("to", TO))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("CUSTOMER_NOT_FOUND"));
	}

	@Test
	void returnsUnauthorizedWithAuthenticationRequiredWhenNoTokenIsSupplied() throws Exception {
		mockMvc.perform(get("/api/v1/customers/{customerId}/summary", CUSTOMER_ID).param("from", FROM).param("to", TO))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	void returnsForbiddenWithAccessDeniedWhenTheAuthorityIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/customers/{customerId}/summary", CUSTOMER_ID)
						.with(jwt().jwt(builder -> builder.subject("jwt-subject-001")).authorities(new SimpleGrantedAuthority("TRANSACTION_WRITE")))
						.param("from", FROM).param("to", TO))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
	}

	@Test
	void grantsAccessWhenMultipleRolesAreCombinedIncludingAggregationRead() throws Exception {
		when(getCustomerSummaryUseCase.get(eq(CUSTOMER_ID), any(), any())).thenReturn(new CustomerSummaryView(
				CUSTOMER_ID, LocalDate.parse(FROM), LocalDate.parse(TO), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "ZAR", 0));

		mockMvc.perform(get("/api/v1/customers/{customerId}/summary", CUSTOMER_ID)
						.with(jwt().jwt(builder -> builder.subject("jwt-subject-001"))
								.authorities(new SimpleGrantedAuthority("CUSTOMER_READ"), new SimpleGrantedAuthority("AGGREGATION_READ")))
						.param("from", FROM).param("to", TO))
				.andExpect(status().isOk());
	}

}
