package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One row of the transaction search response, matching SAD 35.4's documented shape exactly:
 * {@code merchantName} as a plain string (not a nested object), {@code categoryCode} only (no
 * name), no {@code sourceCode}/description/status/receivedAt/createdAt.
 */
public record TransactionSearchItemResponse(
		@Schema(description = "Unique identifier of the transaction.") UUID id,
		@Schema(description = "Identifier of the customer the transaction belongs to.") UUID customerId,
		@Schema(description = "Resolved merchant display name, or null when no merchant was resolved.", nullable = true) String merchantName,
		@Schema(description = "Assigned category code.", example = "GROCERIES") String categoryCode,
		@Schema(description = "Transaction amount, always positive.", example = "450.00") BigDecimal amount,
		@Schema(description = "Three-letter uppercase ISO 4217 currency code.", example = "ZAR") String currency,
		@Schema(description = "Cash-flow direction of the transaction.", allowableValues = {"CREDIT", "DEBIT"}) String direction,
		@Schema(description = "Timestamp the transaction occurred at the source, in UTC.") Instant occurredAt
) {
}
