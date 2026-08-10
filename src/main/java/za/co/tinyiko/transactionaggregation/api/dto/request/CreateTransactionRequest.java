package za.co.tinyiko.transactionaggregation.api.dto.request;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The create-transaction request body (SAD 35.1, TDS 28). {@code merchantName}/{@code description}
 * are the only nullable fields, matching {@code CreateTransactionCommand}'s own nullability.
 *
 * <p>Only structural/cheap checks live here - fast, boundary-level feedback before any
 * cross-module call happens. The authoritative invariants (amount positivity, direction values,
 * currency format, length limits) still live in {@code Transaction.register}/{@code Money}/
 * {@code CreateTransactionService}, unchanged; this is a convenience layer in front of them, not
 * a replacement. {@code direction} is deliberately left as an unconstrained {@code String} here -
 * validating it is exactly {@code CREDIT}/{@code DEBIT} is left to
 * {@code TransactionDirection.valueOf(...)} inside {@code CreateTransactionService}, already
 * built and already tested, rather than duplicating that one check a layer higher.
 */
public record CreateTransactionRequest(
		@Schema(description = "Identifier of the customer the transaction belongs to. Must already exist.",
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull UUID customerId,

		@Schema(description = "Code of the upstream source that supplied this transaction. Must be an active, known source.",
				example = "MOCK_BANK_A", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank String sourceCode,

		@Schema(description = "Unique transaction identifier assigned by the upstream source. Combined with sourceCode, used for duplicate detection (BR-08).",
				example = "TXN-2026-000123", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank @Size(max = 150) String externalTransactionId,

		@Schema(description = "Raw merchant name as supplied by the upstream source, before normalisation. Omitted or null when the source provides no merchant text.",
				example = "Checkers Sandton City", nullable = true)
		String merchantName,

		@Schema(description = "Transaction amount. Always positive; the sign of the cash flow is conveyed separately by direction. Normalised to at most 2 decimal places.",
				example = "450.00", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull @Positive BigDecimal amount,

		@Schema(description = "Three-letter uppercase ISO 4217 currency code.", example = "ZAR",
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "must be a three-letter uppercase ISO 4217 code") String currency,

		@Schema(description = "Cash-flow direction of the transaction.", allowableValues = {"CREDIT", "DEBIT"},
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank String direction,

		@Schema(description = "Free-text transaction description as supplied by the upstream source; used by categorisation rules matching on DESCRIPTION.",
				example = "SALARY - MARCH 2026", nullable = true)
		String description,

		@Schema(description = "Timestamp the transaction occurred at the source, in UTC.", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull Instant occurredAt
) {
}
