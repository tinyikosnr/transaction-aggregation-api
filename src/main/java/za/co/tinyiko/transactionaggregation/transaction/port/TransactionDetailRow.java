package za.co.tinyiko.transactionaggregation.transaction.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * {@code transaction.port}'s own internal projection for a single transaction, returned by
 * {@link TransactionSearchRepositoryPort#findDetailById}. Deliberately not
 * {@code transaction.application.TransactionDetails} - the same "port gets its own row types,
 * never an application-layer type" rule already enforced for {@code TransactionSummaryRepositoryPort}'s
 * row types (see {@code TransactionArchitectureTests#portDoesNotReferenceApplication}).
 *
 * <p>{@code sourceCode} is resolved via an intra-module join to {@code transaction_sources} at
 * the persistence layer - {@code transaction} owns both tables, so this is not a cross-module
 * concern. {@code merchantId}/{@code categoryId} are left as raw ids: resolving them to
 * displayName/code is a {@code transaction.application} concern (batch-lookup through
 * {@code merchant.application}/{@code categorisation.application}), never done here.
 */
public record TransactionDetailRow(
		UUID id,
		UUID customerId,
		String sourceCode,
		String externalTransactionId,
		UUID merchantId,
		UUID categoryId,
		BigDecimal amount,
		String currency,
		String direction,
		String description,
		Instant occurredAt,
		Instant receivedAt,
		String status,
		Instant createdAt
) {
}
