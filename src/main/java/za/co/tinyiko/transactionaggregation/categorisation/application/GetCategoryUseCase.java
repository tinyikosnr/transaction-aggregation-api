package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Inbound port to look up category code/name by id (TDS 28/SAD 35.1's create-transaction
 * response needs it), and, since {@code feature/transaction-query}, the reverse and batch
 * directions needed by transaction search/get. {@code categoryId}/{@code categoryCode} are raw
 * primitives, not {@code categorisation.domain} types - same boundary-primitives reasoning as
 * {@link CategorisationDecision}.
 *
 * <p>Added in {@code feature/api}: the matched-rule path of {@link CategoriseTransactionUseCase}
 * never loads a full {@code TransactionCategory} (it only ever sees a rule's {@code categoryId}),
 * so this is a genuinely new, separate, on-demand lookup - not something {@code CategorisationDecision}
 * could be extended to carry for free.
 *
 * <p>{@link #findIdByCode} (added in {@code feature/transaction-query}) resolves the
 * {@code categoryCode} search filter to a {@code categoryId} before querying transactions -
 * {@code transaction.persistence} must never join {@code transaction_categories} directly.
 * {@link #getByIds} batch-enriches a page of search results in exactly one call (backed by one
 * SQL {@code IN (...)} query), never one lookup per row.
 */
public interface GetCategoryUseCase {

	CategoryView get(UUID categoryId);

	Optional<UUID> findIdByCode(String categoryCode);

	List<CategoryView> getByIds(Collection<UUID> categoryIds);

}
