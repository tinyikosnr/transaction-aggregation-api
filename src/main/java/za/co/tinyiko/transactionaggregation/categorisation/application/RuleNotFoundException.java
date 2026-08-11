package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.util.UUID;

/**
 * Thrown when no {@code CategorisationRule} exists for the given id (feature/category-admin).
 * {@code RULE_NOT_FOUND} is a new error code, not part of the SAD 39.4 catalogue - no
 * rule-specific not-found code is documented anywhere, so this is an explicit project decision,
 * not a documented contract.
 */
public class RuleNotFoundException extends RuntimeException {

	public RuleNotFoundException(UUID ruleId) {
		super("Categorisation rule not found: " + ruleId);
	}

}
