package za.co.tinyiko.transactionaggregation.transaction.domain;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The Transaction aggregate (SAD 27.2) - one immutable financial movement received from an
 * external source (SAD 26.3). Immutable once successfully processed (SAD 27.2 invariant,
 * ADR-013); no mutation methods.
 *
 * <p>{@code transactionSourceId} is typed ({@link TransactionSourceId}) since
 * {@code TransactionSource} lives in this same module; {@code customerId}, {@code merchantId}
 * and {@code categoryId} are raw {@code UUID} since those aggregates live in other modules -
 * matching this codebase's established convention of typed ids only within a module and raw
 * UUIDs across a module boundary (see {@code audit.domain.AuditEvent#aggregateId}).
 *
 * <p>No {@code updatedAt}: SAD's own {@code transactions} table (27.2, 29.3, the ER diagram)
 * omits it entirely, consistent with immutability - there is nothing to timestamp a later
 * update with, because there is no later update.
 *
 * <p>In this branch's synchronous ingestion flow, {@link #register} is the only way a
 * {@code Transaction} is created, and it always assigns {@link TransactionStatus#PROCESSED} -
 * see {@link TransactionStatus}'s Javadoc for why {@code RECEIVED}/{@code REJECTED} exist as
 * enum values without ever being produced by current code paths.
 */
public final class Transaction {

	private static final int MAX_EXTERNAL_TRANSACTION_ID_LENGTH = 150;
	private static final int MAX_DESCRIPTION_LENGTH = 500;
	private static final Duration MAX_FUTURE_TOLERANCE = Duration.ofDays(1);

	private final TransactionId id;
	private final UUID customerId;
	private final TransactionSourceId transactionSourceId;
	private final String externalTransactionId;
	private final UUID merchantId;
	private final UUID categoryId;
	private final Money amount;
	private final TransactionDirection direction;
	private final String description;
	private final Instant occurredAt;
	private final Instant receivedAt;
	private final TransactionStatus status;
	private final Instant createdAt;

	private Transaction(
			TransactionId id,
			UUID customerId,
			TransactionSourceId transactionSourceId,
			String externalTransactionId,
			UUID merchantId,
			UUID categoryId,
			Money amount,
			TransactionDirection direction,
			String description,
			Instant occurredAt,
			Instant receivedAt,
			TransactionStatus status,
			Instant createdAt
	) {
		this.id = id;
		this.customerId = customerId;
		this.transactionSourceId = transactionSourceId;
		this.externalTransactionId = externalTransactionId;
		this.merchantId = merchantId;
		this.categoryId = categoryId;
		this.amount = amount;
		this.direction = direction;
		this.description = description;
		this.occurredAt = occurredAt;
		this.receivedAt = receivedAt;
		this.status = status;
		this.createdAt = createdAt;
	}

	public static Transaction register(
			TransactionId id,
			UUID customerId,
			TransactionSourceId transactionSourceId,
			String externalTransactionId,
			UUID merchantId,
			UUID categoryId,
			Money amount,
			TransactionDirection direction,
			String description,
			Instant occurredAt,
			Clock clock
	) {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(customerId, "customerId must not be null");
		Objects.requireNonNull(transactionSourceId, "transactionSourceId must not be null");
		Objects.requireNonNull(categoryId, "categoryId must not be null");
		Objects.requireNonNull(amount, "amount must not be null");
		Objects.requireNonNull(direction, "direction must not be null");
		Objects.requireNonNull(occurredAt, "occurredAt must not be null");
		Objects.requireNonNull(clock, "clock must not be null");
		String validatedExternalTransactionId = requireInBounds(
				externalTransactionId, "externalTransactionId", MAX_EXTERNAL_TRANSACTION_ID_LENGTH);
		String validatedDescription = validateDescription(description);
		if (amount.amount().signum() <= 0) {
			throw new IllegalArgumentException("amount must be greater than zero");
		}

		Instant now = Instant.now(clock);
		if (occurredAt.isAfter(now.plus(MAX_FUTURE_TOLERANCE))) {
			throw new IllegalArgumentException("occurredAt must not be unreasonably far in the future");
		}

		return new Transaction(id, customerId, transactionSourceId, validatedExternalTransactionId, merchantId,
				categoryId, amount, direction, validatedDescription, occurredAt, now, TransactionStatus.PROCESSED, now);
	}

	public static Transaction reconstitute(
			TransactionId id,
			UUID customerId,
			TransactionSourceId transactionSourceId,
			String externalTransactionId,
			UUID merchantId,
			UUID categoryId,
			Money amount,
			TransactionDirection direction,
			String description,
			Instant occurredAt,
			Instant receivedAt,
			TransactionStatus status,
			Instant createdAt
	) {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(customerId, "customerId must not be null");
		Objects.requireNonNull(transactionSourceId, "transactionSourceId must not be null");
		Objects.requireNonNull(externalTransactionId, "externalTransactionId must not be null");
		Objects.requireNonNull(categoryId, "categoryId must not be null");
		Objects.requireNonNull(amount, "amount must not be null");
		Objects.requireNonNull(direction, "direction must not be null");
		Objects.requireNonNull(occurredAt, "occurredAt must not be null");
		Objects.requireNonNull(receivedAt, "receivedAt must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		return new Transaction(id, customerId, transactionSourceId, externalTransactionId, merchantId, categoryId,
				amount, direction, description, occurredAt, receivedAt, status, createdAt);
	}

	private static String requireInBounds(String value, String fieldName, int maxLength) {
		Objects.requireNonNull(value, fieldName + " must not be null");
		if (value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		if (value.length() > maxLength) {
			throw new IllegalArgumentException(fieldName + " must not exceed " + maxLength + " characters");
		}
		return value;
	}

	private static String validateDescription(String description) {
		if (description == null) {
			return null;
		}
		if (description.length() > MAX_DESCRIPTION_LENGTH) {
			throw new IllegalArgumentException("description must not exceed " + MAX_DESCRIPTION_LENGTH + " characters");
		}
		return description;
	}

	public TransactionId id() {
		return id;
	}

	public UUID customerId() {
		return customerId;
	}

	public TransactionSourceId transactionSourceId() {
		return transactionSourceId;
	}

	public String externalTransactionId() {
		return externalTransactionId;
	}

	public UUID merchantId() {
		return merchantId;
	}

	public UUID categoryId() {
		return categoryId;
	}

	public Money amount() {
		return amount;
	}

	public TransactionDirection direction() {
		return direction;
	}

	public String description() {
		return description;
	}

	public Instant occurredAt() {
		return occurredAt;
	}

	public Instant receivedAt() {
		return receivedAt;
	}

	public TransactionStatus status() {
		return status;
	}

	public Instant createdAt() {
		return createdAt;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof Transaction otherTransaction)) {
			return false;
		}
		return id.equals(otherTransaction.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return "Transaction{id=%s, externalTransactionId=%s, status=%s}".formatted(id, externalTransactionId, status);
	}

}
