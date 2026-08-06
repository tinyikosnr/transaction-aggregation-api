package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.util.Objects;

import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRuleId;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategoryId;

/**
 * The outcome of {@link CategoriseTransactionUseCase#categorise}. {@code matchedRuleId} is
 * {@code null} when the fallback category was assigned - there is no rule to report in that
 * case.
 */
public record CategorisationDecision(TransactionCategoryId categoryId, CategorisationRuleId matchedRuleId, String reason) {

	public CategorisationDecision {
		Objects.requireNonNull(categoryId, "categoryId must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
	}

}
