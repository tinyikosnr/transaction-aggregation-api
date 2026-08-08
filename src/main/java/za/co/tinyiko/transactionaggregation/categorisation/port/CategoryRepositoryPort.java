package za.co.tinyiko.transactionaggregation.categorisation.port;

import java.util.Optional;

import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategory;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategoryId;

/**
 * Outbound port the categorisation application layer uses to read categories.
 *
 * <p>No {@code save}: nothing in this branch creates or updates a category through application
 * code; categories are populated exclusively by the V5 Flyway seed migration.
 *
 * <p>{@code findById} was added in {@code feature/api}, once {@code GetCategoryUseCase} became
 * the first real need to look a category up by id (resolving the code/name for a create-transaction
 * response) - the same read-only concern as {@code findFallback}, on the same table via the same
 * adapter, so it extends this port rather than introducing a separate one.
 */
public interface CategoryRepositoryPort {

	Optional<TransactionCategory> findFallback();

	Optional<TransactionCategory> findById(TransactionCategoryId id);

}
