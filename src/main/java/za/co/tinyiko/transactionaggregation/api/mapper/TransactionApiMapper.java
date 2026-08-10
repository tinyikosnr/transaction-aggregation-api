package za.co.tinyiko.transactionaggregation.api.mapper;

import java.util.List;

import za.co.tinyiko.transactionaggregation.api.dto.request.CreateTransactionRequest;
import za.co.tinyiko.transactionaggregation.api.dto.response.TransactionResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.TransactionSearchItemResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.TransactionSearchResponse;
import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;
import za.co.tinyiko.transactionaggregation.transaction.application.CreateTransactionCommand;
import za.co.tinyiko.transactionaggregation.transaction.application.PagedResult;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionCreatedResult;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionDetails;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionSearchResultItem;

/**
 * Pure structural mapping between {@code api.dto} shapes and {@code transaction.application}
 * contracts - no business logic, matching every other mapper in this codebase (e.g.
 * {@code categorisation.mapper.TransactionCategoryMapper}).
 */
public final class TransactionApiMapper {

	private TransactionApiMapper() {
	}

	public static CreateTransactionCommand toCommand(CreateTransactionRequest request, CorrelationId correlationId, String actor) {
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
				correlationId,
				actor);
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

	/**
	 * {@link TransactionDetails} maps to the same {@link TransactionResponse} shape as
	 * {@link TransactionCreatedResult} - both document identically via SAD 35.1, TDS §30 defines
	 * no separate "get" shape. Kept as a second method, not a shared conversion, since the two
	 * source types are deliberately distinct (see {@code TransactionDetails}'s own Javadoc).
	 */
	public static TransactionResponse toResponse(TransactionDetails details) {
		TransactionResponse.MerchantInfo merchant = details.merchantId() == null
				? null
				: new TransactionResponse.MerchantInfo(details.merchantId(), details.merchantDisplayName());
		TransactionResponse.CategoryInfo category = new TransactionResponse.CategoryInfo(details.categoryCode(), details.categoryName());

		return new TransactionResponse(
				details.id(),
				details.customerId(),
				details.sourceCode(),
				details.externalTransactionId(),
				merchant,
				category,
				details.amount(),
				details.currency(),
				details.direction(),
				details.description(),
				details.status(),
				details.occurredAt(),
				details.receivedAt(),
				details.createdAt());
	}

	public static TransactionSearchResponse toSearchResponse(PagedResult<TransactionSearchResultItem> result) {
		List<TransactionSearchItemResponse> content = result.content().stream()
				.map(TransactionApiMapper::toSearchItemResponse)
				.toList();
		TransactionSearchResponse.PageInfo pageInfo = new TransactionSearchResponse.PageInfo(
				result.page(), result.size(), result.totalElements(), result.totalPages());
		return new TransactionSearchResponse(content, pageInfo);
	}

	private static TransactionSearchItemResponse toSearchItemResponse(TransactionSearchResultItem item) {
		return new TransactionSearchItemResponse(
				item.id(),
				item.customerId(),
				item.merchantName(),
				item.categoryCode(),
				item.amount(),
				item.currency(),
				item.direction(),
				item.occurredAt());
	}

}
