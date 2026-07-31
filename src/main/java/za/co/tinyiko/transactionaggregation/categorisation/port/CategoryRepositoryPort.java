package za.co.tinyiko.transactionaggregation.categorisation.port;

import java.util.Optional;

import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategory;

/**
 * Outbound port the categorisation application layer uses to read categories.
 *
 * <p>Deliberately minimal - only {@code findFallback}, the one query the application layer
 * actually needs. No {@code save}: nothing in this branch creates or updates a category through
 * application code; categories are populated exclusively by the V5 Flyway seed migration. No
 * {@code findById}: nothing currently needs to look one up by id either.
 */
public interface CategoryRepositoryPort {

	Optional<TransactionCategory> findFallback();

}
