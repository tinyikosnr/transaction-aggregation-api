package za.co.tinyiko.transactionaggregation.categorisation.port;

import java.util.List;

import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRule;

/**
 * Outbound port the categorisation application layer uses to read rules. Also minimal - only
 * the one query {@link za.co.tinyiko.transactionaggregation.categorisation.application.CategorisationService}
 * needs. No {@code save}: rules are populated exclusively by the V6 Flyway seed migration.
 */
public interface CategorisationRuleRepositoryPort {

	/**
	 * All active rules. Not required to be pre-sorted by priority - the rule engine sorts
	 * defensively - but the adapter does order by priority anyway, since that's exactly what
	 * the underlying index (idx_categorisation_rules_active_priority) is for.
	 */
	List<CategorisationRule> findAllActive();

}
