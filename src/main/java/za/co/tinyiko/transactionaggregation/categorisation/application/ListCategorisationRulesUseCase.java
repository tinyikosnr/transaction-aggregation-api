package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.util.List;

/**
 * Inbound port to list every categorisation rule (feature/category-admin), regardless of
 * {@code active} status - unlike {@link CategoriseTransactionUseCase}'s own internal read path
 * ({@code CategorisationRuleRepositoryPort#findAllActive}), which this does not touch or reuse.
 */
public interface ListCategorisationRulesUseCase {

	List<CategorisationRuleView> list();

}
