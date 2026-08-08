package za.co.tinyiko.transactionaggregation.api.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import za.co.tinyiko.transactionaggregation.api.dto.request.CreateTransactionRequest;
import za.co.tinyiko.transactionaggregation.api.dto.response.TransactionResponse;
import za.co.tinyiko.transactionaggregation.api.mapper.TransactionApiMapper;
import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;
import za.co.tinyiko.transactionaggregation.transaction.application.CreateTransactionCommand;
import za.co.tinyiko.transactionaggregation.transaction.application.CreateTransactionUseCase;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionCreatedResult;

/**
 * Single documented transaction endpoint this branch implements (SAD 35.1, TDS 28) - bulk
 * creation, retrieval and search are excluded, see the plan's explicit exclusion list. Thin: all
 * orchestration lives in {@link CreateTransactionUseCase}.
 */
@RestController
@RequestMapping("/api/v1/transactions")
class TransactionController {

	private final CreateTransactionUseCase createTransactionUseCase;

	TransactionController(CreateTransactionUseCase createTransactionUseCase) {
		this.createTransactionUseCase = createTransactionUseCase;
	}

	@PostMapping
	ResponseEntity<TransactionResponse> create(@Valid @RequestBody CreateTransactionRequest request, HttpServletRequest servletRequest) {
		CorrelationId correlationId = (CorrelationId) servletRequest.getAttribute(CorrelationId.REQUEST_ATTRIBUTE_NAME);
		CreateTransactionCommand command = TransactionApiMapper.toCommand(request, correlationId);
		TransactionCreatedResult result = createTransactionUseCase.create(command);
		TransactionResponse response = TransactionApiMapper.toResponse(result);

		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(result.id())
				.toUri();
		return ResponseEntity.created(location).body(response);
	}

}
