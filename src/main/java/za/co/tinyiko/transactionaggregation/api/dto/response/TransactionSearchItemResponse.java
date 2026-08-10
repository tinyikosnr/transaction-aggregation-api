package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One row of the transaction search response, matching SAD 35.4's documented shape exactly:
 * {@code merchantName} as a plain string (not a nested object), {@code categoryCode} only (no
 * name), no {@code sourceCode}/description/status/receivedAt/createdAt.
 */
public record TransactionSearchItemResponse(
		UUID id,
		UUID customerId,
		String merchantName,
		String categoryCode,
		BigDecimal amount,
		String currency,
		String direction,
		Instant occurredAt
) {
}
