package za.co.tinyiko.transactionaggregation.transaction.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The outcome of {@link GetTransactionUseCase#get} - the full SAD 35.1 resource shape, reused
 * as-is (TDS §30 documents no separate shape for "get" beyond "returns a single transaction").
 *
 * <p>Deliberately a distinct type from {@link TransactionCreatedResult}, not a shared
 * {@code TransactionView}, even though the two are structurally identical: {@code create} and
 * {@code get} are genuinely different operations (one may write a row, the other never does), and
 * inspection found no architectural reason to unify them beyond eliminating the duplication of a
 * 16-field record - not sufficient reason on its own. {@code api.mapper.TransactionApiMapper} maps
 * both to the same {@code api.dto.response.TransactionResponse}.
 */
public record TransactionDetails(
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
