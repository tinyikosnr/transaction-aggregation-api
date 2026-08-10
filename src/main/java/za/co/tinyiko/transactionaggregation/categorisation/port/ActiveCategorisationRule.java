package za.co.tinyiko.transactionaggregation.categorisation.port;

import java.util.Objects;

import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRule;

/**
 * One active rule together with whether its target category is the fallback category
 * (feature/observability). This port's own shape, never a field on the domain
 * {@link CategorisationRule} itself: matching content (what {@code CategorisationRuleEngine}
 * cares about) and "is the referenced category the fallback one" (a fact about the category, not
 * the rule) are different concerns - denormalising the latter onto the rule aggregate would leak
 * a read-side convenience into the domain model. Existing precedent for a port returning its own
 * shape rather than the bare domain type: {@link CategorisationRuleRow} already does this for the
 * admin read path.
 *
 * <p>Exists specifically so {@link CategorisationRuleRepositoryPort#findAllActive()} can answer
 * "is the winning rule's category the fallback category" from the single query it already runs
 * unconditionally on every categorisation call, with no second, per-categorisation database
 * lookup - see {@code CategorisationService} for how the fallback lookup ({@code
 * CategoryRepositoryPort#findFallback()}) stays confined to the one branch that genuinely needs
 * it (no rule matched at all).
 */
public record ActiveCategorisationRule(CategorisationRule rule, boolean categoryIsFallback) {

	public ActiveCategorisationRule {
		Objects.requireNonNull(rule, "rule must not be null");
	}

}
