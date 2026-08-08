package za.co.tinyiko.transactionaggregation.transaction.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The outcome of {@link CreateTransactionUseCase#create}, matching SAD 35.1's documented
 * create-transaction response shape. Primitives only, same boundary-primitives reasoning as
 * every other cross-module inbound port's result type in this codebase - {@code api} is this
 * port's first real external caller, so (as with {@code CategorisationDecision},
 * {@code MerchantResolutionResult}, {@code CustomerTransactionTotals} before it) it gets its own
 * flat result type here rather than exposing the {@code transaction.domain.Transaction}
 * aggregate directly.
 *
 * <p>{@code merchantId}/{@code merchantDisplayName} are both {@code null} together when no
 * merchant was resolved (no merchant name was supplied) - mirroring {@code Transaction.merchantId}'s
 * own existing nullability, not a new concept.
 */
public record TransactionCreatedResult(
		UUID id,
		UUID customerId,
		String sourceCode,
		String externalTransactionId,
		UUID merchantId,
		String merchantDisplayName,
		String categoryCode,
		String categoryName,
		BigDecimal amount,
		String currency,
		String direction,
		String description,
		String status,
		Instant occurredAt,
		Instant receivedAt,
		Instant createdAt
) {
}
