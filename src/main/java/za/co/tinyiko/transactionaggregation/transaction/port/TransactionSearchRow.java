package za.co.tinyiko.transactionaggregation.transaction.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * {@code transaction.port}'s own internal projection for one row of a search result page - see
 * {@link TransactionDetailRow}'s Javadoc for why this is a separate, port-local type rather than
 * an application-layer one. Deliberately narrower than {@link TransactionDetailRow}: matches
 * SAD 35.4's documented search-row shape exactly (no {@code sourceCode}, no description, no
 * status, no receivedAt/createdAt) - fields the response never surfaces are never fetched.
 */
public record TransactionSearchRow(
		UUID id,
		UUID customerId,
		UUID merchantId,
		UUID categoryId,
		BigDecimal amount,
		String currency,
		String direction,
		Instant occurredAt
) {
}
