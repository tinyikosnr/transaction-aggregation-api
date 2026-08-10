package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The create-transaction response body, matching SAD 35.1's documented shape (the authoritative
 * source - see the plan's note on its conflict with TDS 28's narrower one). {@code merchant} is
 * {@code null} when no merchant was resolved for the transaction.
 */
public record TransactionResponse(
		@Schema(description = "Server-assigned unique identifier of the created/retrieved transaction.")
		UUID id,

		@Schema(description = "Identifier of the customer the transaction belongs to.")
		UUID customerId,

		@Schema(description = "Code of the upstream source that supplied this transaction.", example = "MOCK_BANK_A")
		String sourceCode,

		@Schema(description = "Transaction identifier assigned by the upstream source.", example = "TXN-2026-000123")
		String externalTransactionId,

		@Schema(description = "Resolved merchant, or null when no merchant was resolved for this transaction.", nullable = true)
		MerchantInfo merchant,

		@Schema(description = "Category assigned by the categorisation engine (always present - falls back to the seeded fallback category when no rule matches).")
		CategoryInfo category,

		@Schema(description = "Transaction amount, always positive.", example = "450.00")
		BigDecimal amount,

		@Schema(description = "Three-letter uppercase ISO 4217 currency code.", example = "ZAR")
		String currency,

		@Schema(description = "Cash-flow direction of the transaction.", allowableValues = {"CREDIT", "DEBIT"})
		String direction,

		@Schema(description = "Free-text transaction description as supplied by the upstream source.", nullable = true)
		String description,

		@Schema(description = "Persisted lifecycle status. Only PROCESSED is ever returned by this endpoint - RECEIVED/REJECTED never reach a persisted Transaction in this synchronous flow.",
				allowableValues = {"RECEIVED", "PROCESSED", "REJECTED"})
		String status,

		@Schema(description = "Timestamp the transaction occurred at the source, in UTC.")
		Instant occurredAt,

		@Schema(description = "Timestamp this API received the transaction, in UTC.")
		Instant receivedAt,

		@Schema(description = "Timestamp the transaction row was persisted, in UTC.")
		Instant createdAt
) {

	public record MerchantInfo(
			@Schema(description = "Identifier of the resolved merchant.") UUID id,
			@Schema(description = "Normalised, human-readable merchant display name.", example = "Checkers") String displayName
	) {
	}

	public record CategoryInfo(
			@Schema(description = "Category code.", example = "GROCERIES") String code,
			@Schema(description = "Category display name.", example = "Groceries") String name
	) {
	}

}
