package za.co.tinyiko.transactionaggregation.api.controller;

import java.net.URI;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import za.co.tinyiko.transactionaggregation.api.dto.request.BulkCreateTransactionsRequest;
import za.co.tinyiko.transactionaggregation.api.dto.request.CreateTransactionRequest;
import za.co.tinyiko.transactionaggregation.api.dto.response.BulkTransactionResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.TransactionResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.TransactionSearchResponse;
import za.co.tinyiko.transactionaggregation.api.mapper.TransactionApiMapper;
import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;
import za.co.tinyiko.transactionaggregation.transaction.application.BulkCreateTransactionsCommand;
import za.co.tinyiko.transactionaggregation.transaction.application.BulkTransactionResult;
import za.co.tinyiko.transactionaggregation.transaction.application.CreateTransactionCommand;
import za.co.tinyiko.transactionaggregation.transaction.application.CreateTransactionUseCase;
import za.co.tinyiko.transactionaggregation.transaction.application.CreateTransactionsBulkUseCase;
import za.co.tinyiko.transactionaggregation.transaction.application.GetTransactionUseCase;
import za.co.tinyiko.transactionaggregation.transaction.application.PagedResult;
import za.co.tinyiko.transactionaggregation.transaction.application.SearchTransactionsUseCase;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionCreatedResult;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionDetails;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionSearchCriteria;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionSearchResultItem;

/**
 * {@code Principal} - a plain JDK type, not a Spring Security one - is enough to read the
 * authenticated actor: {@code JwtAuthenticationToken.getName()} returns the JWT {@code sub} claim
 * by Spring Security's own standard behaviour. Since every endpoint requires authentication
 * ({@code @PreAuthorize} below, plus the filter chain's {@code anyRequest().authenticated()}),
 * {@code principal} is guaranteed non-null by the time a method body runs.
 *
 * <p>{@code get}/{@code search} (FR-08, UC-03, UC-04, TDS §30-31) are thin: all filter/pagination/
 * sort validation lives in {@code transaction.application.SearchTransactionsService}, not here.
 *
 * <p>{@code createBulk} (FR-02, UC-02, SAD 32.5/35.2/39.8, TDS §29) is equally thin: it maps the
 * request, delegates once to {@code CreateTransactionsBulkUseCase}, and maps the complete result
 * straight to the response - all per-item validation, processing, and outcome-counting happens in
 * {@code transaction.application.BulkCreateTransactionsService}, never here.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Ingest and query standardised transactions (requires TRANSACTION_WRITE for writes, TRANSACTION_READ for reads).")
class TransactionController {

	private final CreateTransactionUseCase createTransactionUseCase;
	private final CreateTransactionsBulkUseCase createTransactionsBulkUseCase;
	private final GetTransactionUseCase getTransactionUseCase;
	private final SearchTransactionsUseCase searchTransactionsUseCase;

	TransactionController(
			CreateTransactionUseCase createTransactionUseCase,
			CreateTransactionsBulkUseCase createTransactionsBulkUseCase,
			GetTransactionUseCase getTransactionUseCase,
			SearchTransactionsUseCase searchTransactionsUseCase
	) {
		this.createTransactionUseCase = createTransactionUseCase;
		this.createTransactionsBulkUseCase = createTransactionsBulkUseCase;
		this.getTransactionUseCase = getTransactionUseCase;
		this.searchTransactionsUseCase = searchTransactionsUseCase;
	}

	@PostMapping
	@PreAuthorize("hasAuthority('TRANSACTION_WRITE')")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Create a single transaction", description = """
			Validates, deduplicates (by sourceCode + externalTransactionId, BR-08), categorises, and \
			persists a single transaction. Requires the TRANSACTION_WRITE authority.""")
	@ApiResponse(responseCode = "201", description = "Transaction created; Location header points to GET /api/v1/transactions/{id}.")
	@ApiResponse(responseCode = "400", description = "REQUEST_VALIDATION_FAILED - request failed structural or business validation.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "401", description = "AUTHENTICATION_REQUIRED - missing or invalid bearer token.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "403", description = "ACCESS_DENIED - authenticated but missing TRANSACTION_WRITE.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "404", description = "SOURCE_NOT_FOUND or CUSTOMER_NOT_FOUND - sourceCode/customerId does not resolve.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "409", description = "TRANSACTION_DUPLICATE - sourceCode + externalTransactionId already processed (BR-08).", content = @Content(mediaType = "application/problem+json"))
	ResponseEntity<TransactionResponse> create(@Valid @RequestBody CreateTransactionRequest request, HttpServletRequest servletRequest, Principal principal) {
		CorrelationId correlationId = (CorrelationId) servletRequest.getAttribute(CorrelationId.REQUEST_ATTRIBUTE_NAME);
		CreateTransactionCommand command = TransactionApiMapper.toCommand(request, correlationId, principal.getName());
		TransactionCreatedResult result = createTransactionUseCase.create(command);
		TransactionResponse response = TransactionApiMapper.toResponse(result);

		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(result.id())
				.toUri();
		return ResponseEntity.created(location).body(response);
	}

	@PostMapping("/bulk")
	@PreAuthorize("hasAuthority('TRANSACTION_WRITE')")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Create up to 500 transactions in one batch", description = """
			Processes 1-500 transactions, each in its own transaction boundary: one invalid or duplicate \
			item is reported as its own failed/conflict result and never rolls back the rest of the batch \
			(no atomic all-or-nothing guarantee). Always returns 207 Multi-Status once per-item processing \
			starts, whether every item succeeded, every item failed, or the outcome was mixed. Requires the \
			TRANSACTION_WRITE authority.""")
	@ApiResponse(responseCode = "207", description = "Multi-Status - per-item results in the response body; see each item's own status/errorCode.")
	@ApiResponse(responseCode = "400", description = "REQUEST_VALIDATION_FAILED - the batch envelope itself is empty or exceeds 500 items.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "401", description = "AUTHENTICATION_REQUIRED - missing or invalid bearer token.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "403", description = "ACCESS_DENIED - authenticated but missing TRANSACTION_WRITE.", content = @Content(mediaType = "application/problem+json"))
	ResponseEntity<BulkTransactionResponse> createBulk(@Valid @RequestBody BulkCreateTransactionsRequest request, HttpServletRequest servletRequest, Principal principal) {
		CorrelationId correlationId = (CorrelationId) servletRequest.getAttribute(CorrelationId.REQUEST_ATTRIBUTE_NAME);
		BulkCreateTransactionsCommand command = TransactionApiMapper.toBulkCommand(request, correlationId, principal.getName());
		BulkTransactionResult result = createTransactionsBulkUseCase.create(command);
		BulkTransactionResponse response = TransactionApiMapper.toBulkResponse(result);
		return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(response);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('TRANSACTION_READ')")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Get a single transaction by id", description = "Returns full transaction detail, including resolved merchant/category. Requires the TRANSACTION_READ authority.")
	@ApiResponse(responseCode = "200", description = "Transaction found.")
	@ApiResponse(responseCode = "401", description = "AUTHENTICATION_REQUIRED - missing or invalid bearer token.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "403", description = "ACCESS_DENIED - authenticated but missing TRANSACTION_READ.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "404", description = "TRANSACTION_NOT_FOUND - no transaction exists with the given id.", content = @Content(mediaType = "application/problem+json"))
	TransactionResponse get(
			@Parameter(description = "Transaction identifier.") @PathVariable UUID id
	) {
		TransactionDetails details = getTransactionUseCase.get(id);
		return TransactionApiMapper.toResponse(details);
	}

	@GetMapping
	@PreAuthorize("hasAuthority('TRANSACTION_READ')")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Search transactions", description = """
			Filtered, paginated, sorted transaction search. Every filter is optional and combined with AND. \
			An unresolvable sourceCode/categoryCode yields a 200 response with an empty page, not a 404 - \
			an unknown filter value is treated as "no matches", not as a client error. When both occurredFrom \
			and occurredTo are supplied, the range must not exceed 24 months; supplying only one bound is \
			unrestricted. Sorting is restricted to transactionTimestamp (maps to occurredAt), asc or desc; \
			no other field is sortable. Requires the TRANSACTION_READ authority.""")
	@ApiResponse(responseCode = "200", description = "Search executed (possibly with an empty page); see PageInfo for pagination metadata.")
	@ApiResponse(responseCode = "400", description = "REQUEST_VALIDATION_FAILED - invalid filter/sort value, or INVALID_DATE_RANGE when occurredFrom/occurredTo together exceed 24 months.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "401", description = "AUTHENTICATION_REQUIRED - missing or invalid bearer token.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "403", description = "ACCESS_DENIED - authenticated but missing TRANSACTION_READ.", content = @Content(mediaType = "application/problem+json"))
	TransactionSearchResponse search(
			@Parameter(description = "Filter by customer id.") @RequestParam(required = false) UUID customerId,
			@Parameter(description = "Filter by upstream source code. Unresolvable codes yield an empty page, not a 404.", example = "MOCK_BANK_A")
			@RequestParam(required = false) String sourceCode,
			@Parameter(description = "Filter by category code. Unresolvable codes yield an empty page, not a 404.", example = "GROCERIES")
			@RequestParam(required = false) String categoryCode,
			@Parameter(description = "Filter by resolved merchant id.") @RequestParam(required = false) UUID merchantId,
			@Parameter(description = "Filter by cash-flow direction.", schema = @Schema(allowableValues = {"CREDIT", "DEBIT"}))
			@RequestParam(required = false) String direction,
			@Parameter(description = "Filter by persisted status.", schema = @Schema(allowableValues = {"RECEIVED", "PROCESSED", "REJECTED"}))
			@RequestParam(required = false) String status,
			@Parameter(description = "Inclusive lower bound on occurredAt (UTC). Combined with occurredTo, the range must not exceed 24 months.")
			@RequestParam(required = false) Instant occurredFrom,
			@Parameter(description = "Inclusive upper bound on occurredAt (UTC). Combined with occurredFrom, the range must not exceed 24 months.")
			@RequestParam(required = false) Instant occurredTo,
			@Parameter(description = "Zero-based page number.") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Page size.") @RequestParam(defaultValue = "20") int size,
			@Parameter(description = "Sort as 'transactionTimestamp,asc' or 'transactionTimestamp,desc'. No other field is sortable.", example = "transactionTimestamp,desc")
			@RequestParam(required = false) String sort
	) {
		TransactionSearchCriteria criteria = new TransactionSearchCriteria(
				customerId, sourceCode, categoryCode, merchantId, direction, status,
				occurredFrom, occurredTo, page, size, sort);
		PagedResult<TransactionSearchResultItem> result = searchTransactionsUseCase.search(criteria);
		return TransactionApiMapper.toSearchResponse(result);
	}

}
