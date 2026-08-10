package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.util.Objects;
import java.util.UUID;

/**
 * The outcome of {@link CategoriseTransactionUseCase#categorise}. {@code matchedRuleId} is
 * {@code null} only when the rule engine matched no rule at all - including the seeded
 * direction-wide catch-all rules (TDS 37, priority 999/1000) - and the true structural fallback
 * branch in {@code CategorisationService} was reached; under normal seeded data this is
 * essentially never true, since the DEBIT catch-all rule itself already assigns the fallback
 * category via a real rule match.
 *
 * <p>{@code fallbackApplied} (feature/observability) is the field that actually answers "was the
 * fallback category assigned" - {@code matchedRuleId == null} does <strong>not</strong>, precisely
 * because that DEBIT catch-all rule match is common and legitimate, not an edge case. Computed
 * entirely within {@code categorisation} (this module owns the concept of which category is the
 * fallback one), from data already fetched by the same query {@code CategorisationService}
 * already runs unconditionally - see {@code categorisation.port.ActiveCategorisationRule}.
 *
 * <p>{@code categoryId}/{@code matchedRuleId} are raw {@code UUID}, not
 * {@code categorisation.domain}'s {@code TransactionCategoryId}/{@code CategorisationRuleId} -
 * same reasoning as {@link CategorisationInput#direction}: this package is exposed cross-module,
 * {@code categorisation.domain} is not, and a caller reading these ids back has no reason to
 * need the domain-typed wrappers.
 */
public record CategorisationDecision(UUID categoryId, UUID matchedRuleId, String reason, boolean fallbackApplied) {

	public CategorisationDecision {
		Objects.requireNonNull(categoryId, "categoryId must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
	}

}
