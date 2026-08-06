package za.co.tinyiko.transactionaggregation.categorisation.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * The TransactionCategory aggregate (SAD 27.5). Following SAD's shape over TDS's: SAD's
 * {@code description}/{@code fallback} fields are kept, TDS's {@code type} field is dropped -
 * it doesn't drive any documented business logic (aggregation's income/expenditure split comes
 * from transaction direction, per BR-12/13, not category type).
 *
 * <p>No behaviour methods: neither SAD nor TDS documents a lifecycle for a category beyond
 * creation. Immutable after construction, same as {@code Merchant}.
 */
public final class TransactionCategory {

	private static final int MAX_CODE_LENGTH = 50;
	private static final int MAX_NAME_LENGTH = 100;
	private static final int MAX_DESCRIPTION_LENGTH = 500;

	private final TransactionCategoryId id;
	private final String code;
	private final String name;
	private final String description;
	private final boolean fallback;
	private final boolean active;
	private final Instant createdAt;
	private final Instant updatedAt;

	private TransactionCategory(
			TransactionCategoryId id,
			String code,
			String name,
			String description,
			boolean fallback,
			boolean active,
			Instant createdAt,
			Instant updatedAt
	) {
		this.id = id;
		this.code = code;
		this.name = name;
		this.description = description;
		this.fallback = fallback;
		this.active = active;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public static TransactionCategory register(
			TransactionCategoryId id,
			String code,
			String name,
			String description,
			boolean fallback,
			Clock clock
	) {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(clock, "clock must not be null");
		String validatedCode = requireInBounds(code, "code", MAX_CODE_LENGTH);
		String validatedName = requireInBounds(name, "name", MAX_NAME_LENGTH);
		String validatedDescription = validateDescription(description);

		Instant now = Instant.now(clock);
		return new TransactionCategory(id, validatedCode, validatedName, validatedDescription, fallback, true, now, now);
	}

	public static TransactionCategory reconstitute(
			TransactionCategoryId id,
			String code,
			String name,
			String description,
			boolean fallback,
			boolean active,
			Instant createdAt,
			Instant updatedAt
	) {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(code, "code must not be null");
		Objects.requireNonNull(name, "name must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		return new TransactionCategory(id, code, name, description, fallback, active, createdAt, updatedAt);
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

	public TransactionCategoryId id() {
		return id;
	}

	public String code() {
		return code;
	}

	public String name() {
		return name;
	}

	public String description() {
		return description;
	}

	public boolean fallback() {
		return fallback;
	}

	public boolean active() {
		return active;
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
		if (!(other instanceof TransactionCategory otherCategory)) {
			return false;
		}
		return id.equals(otherCategory.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return "TransactionCategory{id=%s, code=%s, fallback=%s}".formatted(id, code, fallback);
	}

}
