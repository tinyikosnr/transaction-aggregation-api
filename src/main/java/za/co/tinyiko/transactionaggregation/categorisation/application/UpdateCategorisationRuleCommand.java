package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.util.Objects;
import java.util.UUID;

/**
 * The data required to update a categorisation rule (feature/category-admin, TDS 55's
 * {@code UpdateCategorisationRuleRequest}) - full-replacement semantics (SAD 34.2's {@code PUT}:
 * "replace a complete mutable resource"), every mutable field required, including {@code active}
 * (this is the only way to activate/deactivate or change priority - there is no separate
 * endpoint for either, nothing in the documentation distinguishes them from an ordinary field
 * edit). {@code expectedVersion} carries the caller's optimistic-locking intent as a plain
 * {@code long} - never a JPA/Hibernate type - compared against the persisted row's current
 * version before any write is attempted.
 */
public record UpdateCategorisationRuleCommand(
		UUID categoryId,
		String matchField,
		String operator,
		String matchValue,
		String direction,
		int priority,
		boolean active,
		long expectedVersion
) {

	public UpdateCategorisationRuleCommand {
		Objects.requireNonNull(categoryId, "categoryId must not be null");
		Objects.requireNonNull(matchField, "matchField must not be null");
		Objects.requireNonNull(operator, "operator must not be null");
		Objects.requireNonNull(matchValue, "matchValue must not be null");
		Objects.requireNonNull(direction, "direction must not be null");
	}

}
