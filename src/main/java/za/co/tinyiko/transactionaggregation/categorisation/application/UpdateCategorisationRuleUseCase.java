package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.util.UUID;

/**
 * Inbound port to update a categorisation rule (feature/category-admin). Full replacement, per
 * {@link UpdateCategorisationRuleCommand}'s own Javadoc. Throws {@link RuleNotFoundException} for
 * an unknown id, {@link RuleConflictException} for a stale {@code expectedVersion}, {@link
 * CategoryNotFoundException} for an unknown {@code categoryId}.
 */
public interface UpdateCategorisationRuleUseCase {

	CategorisationRuleView update(UUID ruleId, UpdateCategorisationRuleCommand command);

}
