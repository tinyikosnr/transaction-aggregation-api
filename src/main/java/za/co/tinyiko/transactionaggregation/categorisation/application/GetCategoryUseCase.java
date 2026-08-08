package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.util.UUID;

/**
 * Inbound port to look up a single category's code/name by id (TDS 28/SAD 35.1's create-transaction
 * response needs it). {@code categoryId} is a raw {@code UUID}, not
 * {@code categorisation.domain.TransactionCategoryId} - same boundary-primitives reasoning as
 * {@link CategorisationDecision}.
 *
 * <p>Added in {@code feature/api}: the matched-rule path of {@link CategoriseTransactionUseCase}
 * never loads a full {@code TransactionCategory} (it only ever sees a rule's {@code categoryId}),
 * so this is a genuinely new, separate, on-demand lookup - not something {@code CategorisationDecision}
 * could be extended to carry for free.
 */
public interface GetCategoryUseCase {

	CategoryView get(UUID categoryId);

}
