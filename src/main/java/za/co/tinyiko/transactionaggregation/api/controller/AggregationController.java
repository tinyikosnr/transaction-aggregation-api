package za.co.tinyiko.transactionaggregation.api.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import za.co.tinyiko.transactionaggregation.aggregation.application.CustomerSummaryView;
import za.co.tinyiko.transactionaggregation.aggregation.application.GetCategorySummaryUseCase;
import za.co.tinyiko.transactionaggregation.aggregation.application.GetCustomerSummaryUseCase;
import za.co.tinyiko.transactionaggregation.aggregation.application.GetMerchantSummaryUseCase;
import za.co.tinyiko.transactionaggregation.aggregation.application.GetMonthlySummaryUseCase;
import za.co.tinyiko.transactionaggregation.api.dto.response.CategorySummaryResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.CustomerSummaryResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.MerchantSummaryResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.MonthlySummaryResponse;
import za.co.tinyiko.transactionaggregation.api.mapper.AggregationApiMapper;

/**
 * The four documented aggregation endpoints (TDS 6, 32-35). Thin: all computation, including
 * {@code from}/{@code to} range validation, lives in the four {@code Get*SummaryUseCase}s -
 * {@code aggregation.domain.DateRange} is not exposed cross-module, so this controller passes
 * the raw {@link LocalDate} query parameters straight through rather than constructing one
 * itself (see {@code GetCustomerSummaryUseCase}'s Javadoc).
 */
@RestController
@RequestMapping("/api/v1/customers/{customerId}")
class AggregationController {

	private final GetCustomerSummaryUseCase getCustomerSummaryUseCase;
	private final GetCategorySummaryUseCase getCategorySummaryUseCase;
	private final GetMerchantSummaryUseCase getMerchantSummaryUseCase;
	private final GetMonthlySummaryUseCase getMonthlySummaryUseCase;

	AggregationController(
			GetCustomerSummaryUseCase getCustomerSummaryUseCase,
			GetCategorySummaryUseCase getCategorySummaryUseCase,
			GetMerchantSummaryUseCase getMerchantSummaryUseCase,
			GetMonthlySummaryUseCase getMonthlySummaryUseCase
	) {
		this.getCustomerSummaryUseCase = getCustomerSummaryUseCase;
		this.getCategorySummaryUseCase = getCategorySummaryUseCase;
		this.getMerchantSummaryUseCase = getMerchantSummaryUseCase;
		this.getMonthlySummaryUseCase = getMonthlySummaryUseCase;
	}

	@GetMapping("/summary")
	@PreAuthorize("hasAuthority('AGGREGATION_READ')")
	CustomerSummaryResponse customerSummary(
			@PathVariable UUID customerId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
	) {
		CustomerSummaryView view = getCustomerSummaryUseCase.get(customerId, from, to);
		return AggregationApiMapper.toResponse(view);
	}

	@GetMapping("/categories")
	@PreAuthorize("hasAuthority('AGGREGATION_READ')")
	List<CategorySummaryResponse> categorySummary(
			@PathVariable UUID customerId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
	) {
		return AggregationApiMapper.toCategoryResponses(getCategorySummaryUseCase.get(customerId, from, to));
	}

	@GetMapping("/merchants")
	@PreAuthorize("hasAuthority('AGGREGATION_READ')")
	List<MerchantSummaryResponse> merchantSummary(
			@PathVariable UUID customerId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
	) {
		return AggregationApiMapper.toMerchantResponses(getMerchantSummaryUseCase.get(customerId, from, to));
	}

	@GetMapping("/monthly-summary")
	@PreAuthorize("hasAuthority('AGGREGATION_READ')")
	List<MonthlySummaryResponse> monthlySummary(
			@PathVariable UUID customerId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
	) {
		return AggregationApiMapper.toMonthlyResponses(getMonthlySummaryUseCase.get(customerId, from, to));
	}

}
