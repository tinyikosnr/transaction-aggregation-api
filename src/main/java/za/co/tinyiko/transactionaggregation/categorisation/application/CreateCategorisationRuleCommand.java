package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.util.Objects;
import java.util.UUID;

/**
 * The data required to create a categorisation rule (feature/category-admin, TDS 55's
 * {@code CreateCategorisationRuleRequest}). Primitives only, matching every other cross-module
 * command in this codebase. {@code active} is not a field here: a newly created rule always
 * starts active, matching {@code CategorisationRule.register}'s existing, unchanged behaviour.
 */
public record CreateCategorisationRuleCommand(
		UUID categoryId,
		String matchField,
		String operator,
		String matchValue,
		String direction,
		int priority
) {

	public CreateCategorisationRuleCommand {
		Objects.requireNonNull(categoryId, "categoryId must not be null");
		Objects.requireNonNull(matchField, "matchField must not be null");
		Objects.requireNonNull(operator, "operator must not be null");
		Objects.requireNonNull(matchValue, "matchValue must not be null");
		Objects.requireNonNull(direction, "direction must not be null");
	}

}
