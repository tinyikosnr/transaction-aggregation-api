package za.co.tinyiko.transactionaggregation.transaction.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * The TransactionSource aggregate (SAD 27.3). Reference/seed data in this branch - populated
 * exclusively by the {@code V10} Flyway seed migration, never through application code, so
 * there is no mutation method here (matching {@code TransactionCategory}'s precedent: a
 * documented {@code status} field with no current write path still gets a plain, immutable
 * representation, not a speculative update capability).
 */
public final class TransactionSource {

	private static final int MAX_CODE_LENGTH = 50;
	private static final int MAX_NAME_LENGTH = 150;

	private final TransactionSourceId id;
	private final String code;
	private final String name;
	private final SourceStatus status;
	private final Instant createdAt;
	private final Instant updatedAt;

	private TransactionSource(
			TransactionSourceId id,
			String code,
			String name,
			SourceStatus status,
			Instant createdAt,
			Instant updatedAt
	) {
		this.id = id;
		this.code = code;
		this.name = name;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public static TransactionSource register(
			TransactionSourceId id,
			String code,
			String name,
			SourceStatus status,
			Clock clock
	) {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(clock, "clock must not be null");
		String validatedCode = requireInBounds(code, "code", MAX_CODE_LENGTH);
		String validatedName = requireInBounds(name, "name", MAX_NAME_LENGTH);

		Instant now = Instant.now(clock);
		return new TransactionSource(id, validatedCode, validatedName, status, now, now);
	}

	public static TransactionSource reconstitute(
			TransactionSourceId id,
			String code,
			String name,
			SourceStatus status,
			Instant createdAt,
			Instant updatedAt
	) {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(code, "code must not be null");
		Objects.requireNonNull(name, "name must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		return new TransactionSource(id, code, name, status, createdAt, updatedAt);
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

	public boolean isActive() {
		return status == SourceStatus.ACTIVE;
	}

	public TransactionSourceId id() {
		return id;
	}

	public String code() {
		return code;
	}

	public String name() {
		return name;
	}

	public SourceStatus status() {
		return status;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof TransactionSource otherSource)) {
			return false;
		}
		return id.equals(otherSource.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return "TransactionSource{id=%s, code=%s, status=%s}".formatted(id, code, status);
	}

}
