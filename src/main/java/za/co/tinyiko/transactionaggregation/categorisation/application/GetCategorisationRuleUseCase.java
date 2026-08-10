package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.util.UUID;

/**
 * Inbound port to get one categorisation rule by id (feature/category-admin). Split from {@link
 * ListCategorisationRulesUseCase} the same way {@code GetTransactionUseCase} is split from
 * {@code SearchTransactionsUseCase} - a genuinely different failure mode ({@link
 * RuleNotFoundException} here, never there).
 */
public interface GetCategorisationRuleUseCase {

	CategorisationRuleView get(UUID ruleId);

}
