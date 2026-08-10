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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "Aggregation", description = "Customer financial summaries: totals, category/merchant breakdowns, and monthly trends (requires AGGREGATION_READ).")
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
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Get a customer's overall financial summary", description = "Total income, expenditure, and net cash flow for the customer within the given date range (both bounds inclusive, max 24-month span). Requires the AGGREGATION_READ authority.")
	@ApiResponse(responseCode = "200", description = "Summary computed (zero/empty totals when the customer has no transactions in range - not a failure).")
	@ApiResponse(responseCode = "400", description = "INVALID_DATE_RANGE - from/to missing, from after to, or the range exceeds 24 months.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "401", description = "AUTHENTICATION_REQUIRED - missing or invalid bearer token.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "403", description = "ACCESS_DENIED - authenticated but missing AGGREGATION_READ.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "404", description = "CUSTOMER_NOT_FOUND - no customer exists with the given id.", content = @Content(mediaType = "application/problem+json"))
	CustomerSummaryResponse customerSummary(
			@Parameter(description = "Identifier of the customer to summarise.") @PathVariable UUID customerId,
			@Parameter(description = "Inclusive start date.") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@Parameter(description = "Inclusive end date.") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
	) {
		CustomerSummaryView view = getCustomerSummaryUseCase.get(customerId, from, to);
		return AggregationApiMapper.toResponse(view);
	}

	@GetMapping("/categories")
	@PreAuthorize("hasAuthority('AGGREGATION_READ')")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Get a customer's category breakdown", description = "Per-category totals for the customer within the given date range (both bounds inclusive, max 24-month span). Requires the AGGREGATION_READ authority.")
	@ApiResponse(responseCode = "200", description = "Breakdown computed (empty list when the customer has no transactions in range - not a failure).")
	@ApiResponse(responseCode = "400", description = "INVALID_DATE_RANGE - from/to missing, from after to, or the range exceeds 24 months.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "401", description = "AUTHENTICATION_REQUIRED - missing or invalid bearer token.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "403", description = "ACCESS_DENIED - authenticated but missing AGGREGATION_READ.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "404", description = "CUSTOMER_NOT_FOUND - no customer exists with the given id.", content = @Content(mediaType = "application/problem+json"))
	List<CategorySummaryResponse> categorySummary(
			@Parameter(description = "Identifier of the customer to summarise.") @PathVariable UUID customerId,
			@Parameter(description = "Inclusive start date.") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@Parameter(description = "Inclusive end date.") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
	) {
		return AggregationApiMapper.toCategoryResponses(getCategorySummaryUseCase.get(customerId, from, to));
	}

	@GetMapping("/merchants")
	@PreAuthorize("hasAuthority('AGGREGATION_READ')")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Get a customer's merchant breakdown", description = "Per-merchant totals for the customer within the given date range (both bounds inclusive, max 24-month span). Requires the AGGREGATION_READ authority.")
	@ApiResponse(responseCode = "200", description = "Breakdown computed (empty list when the customer has no transactions in range - not a failure).")
	@ApiResponse(responseCode = "400", description = "INVALID_DATE_RANGE - from/to missing, from after to, or the range exceeds 24 months.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "401", description = "AUTHENTICATION_REQUIRED - missing or invalid bearer token.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "403", description = "ACCESS_DENIED - authenticated but missing AGGREGATION_READ.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "404", description = "CUSTOMER_NOT_FOUND - no customer exists with the given id.", content = @Content(mediaType = "application/problem+json"))
	List<MerchantSummaryResponse> merchantSummary(
			@Parameter(description = "Identifier of the customer to summarise.") @PathVariable UUID customerId,
			@Parameter(description = "Inclusive start date.") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@Parameter(description = "Inclusive end date.") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
	) {
		return AggregationApiMapper.toMerchantResponses(getMerchantSummaryUseCase.get(customerId, from, to));
	}

	@GetMapping("/monthly-summary")
	@PreAuthorize("hasAuthority('AGGREGATION_READ')")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Get a customer's monthly breakdown", description = "Per-calendar-month totals (UTC) for the customer within the given date range (both bounds inclusive, max 24-month span). Requires the AGGREGATION_READ authority.")
	@ApiResponse(responseCode = "200", description = "Breakdown computed (empty list when the customer has no transactions in range - not a failure).")
	@ApiResponse(responseCode = "400", description = "INVALID_DATE_RANGE - from/to missing, from after to, or the range exceeds 24 months.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "401", description = "AUTHENTICATION_REQUIRED - missing or invalid bearer token.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "403", description = "ACCESS_DENIED - authenticated but missing AGGREGATION_READ.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "404", description = "CUSTOMER_NOT_FOUND - no customer exists with the given id.", content = @Content(mediaType = "application/problem+json"))
	List<MonthlySummaryResponse> monthlySummary(
			@Parameter(description = "Identifier of the customer to summarise.") @PathVariable UUID customerId,
			@Parameter(description = "Inclusive start date.") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@Parameter(description = "Inclusive end date.") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
	) {
		return AggregationApiMapper.toMonthlyResponses(getMonthlySummaryUseCase.get(customerId, from, to));
	}

}
