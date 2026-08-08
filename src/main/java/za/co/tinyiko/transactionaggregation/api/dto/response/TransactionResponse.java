package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The create-transaction response body, matching SAD 35.1's documented shape (the authoritative
 * source - see the plan's note on its conflict with TDS 28's narrower one). {@code merchant} is
 * {@code null} when no merchant was resolved for the transaction.
 */
public record TransactionResponse(
		UUID id,
		UUID customerId,
		String sourceCode,
		String externalTransactionId,
		MerchantInfo merchant,
		CategoryInfo category,
		BigDecimal amount,
		String currency,
		String direction,
		String description,
		String status,
		Instant occurredAt,
		Instant receivedAt,
		Instant createdAt
) {

	public record MerchantInfo(UUID id, String displayName) {
	}

	public record CategoryInfo(String code, String name) {
	}

}
