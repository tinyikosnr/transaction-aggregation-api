package za.co.tinyiko.transactionaggregation.api.mapper;

import za.co.tinyiko.transactionaggregation.api.dto.request.CreateTransactionRequest;
import za.co.tinyiko.transactionaggregation.api.dto.response.TransactionResponse;
import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;
import za.co.tinyiko.transactionaggregation.transaction.application.CreateTransactionCommand;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionCreatedResult;

/**
 * Pure structural mapping between {@code api.dto} shapes and {@code transaction.application}
 * contracts - no business logic, matching every other mapper in this codebase (e.g.
 * {@code categorisation.mapper.TransactionCategoryMapper}).
 */
public final class TransactionApiMapper {

	private TransactionApiMapper() {
	}

	public static CreateTransactionCommand toCommand(CreateTransactionRequest request, CorrelationId correlationId) {
		return new CreateTransactionCommand(
				request.externalTransactionId(),
				request.customerId(),
				request.sourceCode(),
				request.amount(),
				request.currency(),
				request.direction(),
				request.description(),
				request.merchantName(),
				request.occurredAt(),
				correlationId);
	}

	public static TransactionResponse toResponse(TransactionCreatedResult result) {
		TransactionResponse.MerchantInfo merchant = result.merchantId() == null
				? null
				: new TransactionResponse.MerchantInfo(result.merchantId(), result.merchantDisplayName());
		TransactionResponse.CategoryInfo category = new TransactionResponse.CategoryInfo(result.categoryCode(), result.categoryName());

		return new TransactionResponse(
				result.id(),
				result.customerId(),
				result.sourceCode(),
				result.externalTransactionId(),
				merchant,
				category,
				result.amount(),
				result.currency(),
				result.direction(),
				result.description(),
				result.status(),
				result.occurredAt(),
				result.receivedAt(),
				result.createdAt());
	}

}
