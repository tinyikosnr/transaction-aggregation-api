package za.co.tinyiko.transactionaggregation.transaction.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;

/**
 * The data required to create a transaction (TDS 28's request shape). Primitives only,
 * matching {@code RegisterCustomerCommand}'s precedent - {@code description} and
 * {@code merchantName} are nullable (SAD 27.2: description nullable; TDS 10: merchant optional
 * when it cannot be resolved). Detailed validation (lengths, amount positivity, direction
 * values) is left to {@link CreateTransactionService} and {@code Transaction.register}, not
 * duplicated here - only null-checks for fields that are always required.
 *
 * <p>{@code correlationId} is typed as {@code CorrelationId}, matching
 * {@code audit.application.RecordAuditEventCommand}'s existing precedent - added in
 * {@code feature/api} so the id {@code config.CorrelationIdFilter} resolves for the inbound
 * request is the same one that ends up on this transaction's audit trail, rather than
 * {@link CreateTransactionService} generating its own disconnected value (SAD 34.10).
 */
public record CreateTransactionCommand(
		String externalTransactionId,
		UUID customerId,
		String sourceCode,
		BigDecimal amount,
		String currency,
		String direction,
		String description,
		String merchantName,
		Instant occurredAt,
		CorrelationId correlationId
) {

	public CreateTransactionCommand {
		Objects.requireNonNull(externalTransactionId, "externalTransactionId must not be null");
		Objects.requireNonNull(customerId, "customerId must not be null");
		Objects.requireNonNull(sourceCode, "sourceCode must not be null");
		Objects.requireNonNull(amount, "amount must not be null");
		Objects.requireNonNull(currency, "currency must not be null");
		Objects.requireNonNull(direction, "direction must not be null");
		Objects.requireNonNull(occurredAt, "occurredAt must not be null");
		Objects.requireNonNull(correlationId, "correlationId must not be null");
	}

}
