package za.co.tinyiko.transactionaggregation.categorisation.application;

/**
 * Inbound port to create a categorisation rule (feature/category-admin, FR/UC not documented -
 * an explicit project decision derived from TDS 55's named {@code CreateCategorisationRuleRequest}
 * and SAD 36.4's {@code CATEGORY_ADMIN} authority).
 */
public interface CreateCategorisationRuleUseCase {

	CategorisationRuleView create(CreateCategorisationRuleCommand command);

}
