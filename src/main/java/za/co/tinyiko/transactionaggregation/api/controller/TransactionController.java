package za.co.tinyiko.transactionaggregation.api.controller;

import java.net.URI;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

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

import za.co.tinyiko.transactionaggregation.api.dto.request.CreateTransactionRequest;
import za.co.tinyiko.transactionaggregation.api.dto.response.TransactionResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.TransactionSearchResponse;
import za.co.tinyiko.transactionaggregation.api.mapper.TransactionApiMapper;
import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;
import za.co.tinyiko.transactionaggregation.transaction.application.CreateTransactionCommand;
import za.co.tinyiko.transactionaggregation.transaction.application.CreateTransactionUseCase;
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
 */
@RestController
@RequestMapping("/api/v1/transactions")
class TransactionController {

	private final CreateTransactionUseCase createTransactionUseCase;
	private final GetTransactionUseCase getTransactionUseCase;
	private final SearchTransactionsUseCase searchTransactionsUseCase;

	TransactionController(
			CreateTransactionUseCase createTransactionUseCase,
			GetTransactionUseCase getTransactionUseCase,
			SearchTransactionsUseCase searchTransactionsUseCase
	) {
		this.createTransactionUseCase = createTransactionUseCase;
		this.getTransactionUseCase = getTransactionUseCase;
		this.searchTransactionsUseCase = searchTransactionsUseCase;
	}

	@PostMapping
	@PreAuthorize("hasAuthority('TRANSACTION_WRITE')")
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

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('TRANSACTION_READ')")
	TransactionResponse get(@PathVariable UUID id) {
		TransactionDetails details = getTransactionUseCase.get(id);
		return TransactionApiMapper.toResponse(details);
	}

	@GetMapping
	@PreAuthorize("hasAuthority('TRANSACTION_READ')")
	TransactionSearchResponse search(
			@RequestParam(required = false) UUID customerId,
			@RequestParam(required = false) String sourceCode,
			@RequestParam(required = false) String categoryCode,
			@RequestParam(required = false) UUID merchantId,
			@RequestParam(required = false) String direction,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Instant occurredFrom,
			@RequestParam(required = false) Instant occurredTo,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String sort
	) {
		TransactionSearchCriteria criteria = new TransactionSearchCriteria(
				customerId, sourceCode, categoryCode, merchantId, direction, status,
				occurredFrom, occurredTo, page, size, sort);
		PagedResult<TransactionSearchResultItem> result = searchTransactionsUseCase.search(criteria);
		return TransactionApiMapper.toSearchResponse(result);
	}

}
