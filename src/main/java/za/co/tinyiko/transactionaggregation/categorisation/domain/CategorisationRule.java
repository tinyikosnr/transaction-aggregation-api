package za.co.tinyiko.transactionaggregation.categorisation.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The CategorisationRule aggregate (SAD 27.6). Carries its own matching behaviour
 * ({@link #matches}) rather than leaving it to {@link CategorisationRuleEngine} - a rule
 * knowing how to evaluate itself against an input is the rule's own responsibility; the engine
 * is a thin priority-ordered orchestrator over a list of rules.
 *
 * <p>{@code direction} is not part of either SAD's or TDS's documented field list for this
 * entity, but the seed data (TDS 37's Initial Categorisation Matrix) gives every rule a
 * direction, and without it the two direction-wide catch-all rules would be unreachable for
 * one direction. Added as a required field, grounded in the seed data's own requirements.
 *
 * <p>Immutable after creation: rules are only ever populated by the V6 Flyway seed migration in
 * this branch, not created or edited through application code.
 */
public final class CategorisationRule {

	private static final int MAX_MATCH_VALUE_LENGTH = 200;

	private final CategorisationRuleId id;
	private final TransactionCategoryId categoryId;
	private final MatchField matchField;
	private final MatchOperator operator;
	private final String matchValue;
	private final Direction direction;
	private final int priority;
	private final boolean active;
	private final Instant createdAt;
	private final Instant updatedAt;

	private CategorisationRule(
			CategorisationRuleId id,
			TransactionCategoryId categoryId,
			MatchField matchField,
			MatchOperator operator,
			String matchValue,
			Direction direction,
			int priority,
			boolean active,
			Instant createdAt,
			Instant updatedAt
	) {
		this.id = id;
		this.categoryId = categoryId;
		this.matchField = matchField;
		this.operator = operator;
		this.matchValue = matchValue;
		this.direction = direction;
		this.priority = priority;
		this.active = active;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public static CategorisationRule register(
			CategorisationRuleId id,
			TransactionCategoryId categoryId,
			MatchField matchField,
			MatchOperator operator,
			String matchValue,
			Direction direction,
			int priority,
			Clock clock
	) {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(categoryId, "categoryId must not be null");
		Objects.requireNonNull(matchField, "matchField must not be null");
		Objects.requireNonNull(operator, "operator must not be null");
		Objects.requireNonNull(direction, "direction must not be null");
		Objects.requireNonNull(clock, "clock must not be null");
		String validatedMatchValue = requireValidMatchValue(matchValue);
		if (priority <= 0) {
			throw new IllegalArgumentException("priority must be positive");
		}

		Instant now = Instant.now(clock);
		// Stored uppercase, per TDS 14.
		String upperMatchValue = validatedMatchValue.toUpperCase(Locale.ROOT);
		return new CategorisationRule(id, categoryId, matchField, operator, upperMatchValue, direction, priority, true, now, now);
	}

	public static CategorisationRule reconstitute(
			CategorisationRuleId id,
			TransactionCategoryId categoryId,
			MatchField matchField,
			MatchOperator operator,
			String matchValue,
			Direction direction,
			int priority,
			boolean active,
			Instant createdAt,
			Instant updatedAt
	) {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(categoryId, "categoryId must not be null");
		Objects.requireNonNull(matchField, "matchField must not be null");
		Objects.requireNonNull(operator, "operator must not be null");
		Objects.requireNonNull(matchValue, "matchValue must not be null");
		Objects.requireNonNull(direction, "direction must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		return new CategorisationRule(id, categoryId, matchField, operator, matchValue, direction, priority, active, createdAt, updatedAt);
	}

	private static String requireValidMatchValue(String matchValue) {
		Objects.requireNonNull(matchValue, "matchValue must not be null");
		if (matchValue.isBlank()) {
			throw new IllegalArgumentException("matchValue must not be blank");
		}
		if (matchValue.length() > MAX_MATCH_VALUE_LENGTH) {
			throw new IllegalArgumentException("matchValue must not exceed " + MAX_MATCH_VALUE_LENGTH + " characters");
		}
		return matchValue;
	}

	/**
	 * Whether this rule matches the given categorisation input. Does not consider
	 * {@link #active} - callers are expected to only evaluate rules already known to be active
	 * (see {@code CategorisationRuleRepositoryPort#findAllActive}); this method is purely about
	 * content matching.
	 */
	public boolean matches(String merchantText, String description, Direction transactionDirection) {
		Objects.requireNonNull(transactionDirection, "transactionDirection must not be null");
		if (direction != transactionDirection) {
			return false;
		}
		String fieldText = matchField == MatchField.MERCHANT ? merchantText : description;
		if (fieldText == null) {
			return false;
		}
		String upperFieldText = fieldText.toUpperCase(Locale.ROOT);
		return switch (operator) {
			case EQUALS -> upperFieldText.equals(matchValue);
			case CONTAINS -> upperFieldText.contains(matchValue);
			case REGEX -> Pattern.compile(matchValue).matcher(upperFieldText).find();
		};
	}

	public CategorisationRuleId id() {
		return id;
	}

	public TransactionCategoryId categoryId() {
		return categoryId;
	}

	public MatchField matchField() {
		return matchField;
	}

	public MatchOperator operator() {
		return operator;
	}

	public String matchValue() {
		return matchValue;
	}

	public Direction direction() {
		return direction;
	}

	public int priority() {
		return priority;
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
		if (!(other instanceof CategorisationRule otherRule)) {
			return false;
		}
		return id.equals(otherRule.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return "CategorisationRule{id=%s, priority=%d, matchField=%s, operator=%s, matchValue=%s, direction=%s}"
				.formatted(id, priority, matchField, operator, matchValue, direction);
	}

}
