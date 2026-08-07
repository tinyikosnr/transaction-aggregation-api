package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.util.Objects;
import java.util.UUID;

/**
 * The outcome of {@link CategoriseTransactionUseCase#categorise}. {@code matchedRuleId} is
 * {@code null} when the fallback category was assigned - there is no rule to report in that
 * case.
 *
 * <p>{@code categoryId}/{@code matchedRuleId} are raw {@code UUID}, not
 * {@code categorisation.domain}'s {@code TransactionCategoryId}/{@code CategorisationRuleId} -
 * same reasoning as {@link CategorisationInput#direction}: this package is exposed cross-module,
 * {@code categorisation.domain} is not, and a caller reading these ids back has no reason to
 * need the domain-typed wrappers.
 */
public record CategorisationDecision(UUID categoryId, UUID matchedRuleId, String reason) {

	public CategorisationDecision {
		Objects.requireNonNull(categoryId, "categoryId must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
	}

}
