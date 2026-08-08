package za.co.tinyiko.transactionaggregation.api.dto.request;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

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
		@NotNull UUID customerId,
		@NotBlank String sourceCode,
		@NotBlank @Size(max = 150) String externalTransactionId,
		String merchantName,
		@NotNull @Positive BigDecimal amount,
		@NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "must be a three-letter uppercase ISO 4217 code") String currency,
		@NotBlank String direction,
		String description,
		@NotNull Instant occurredAt
) {
}
