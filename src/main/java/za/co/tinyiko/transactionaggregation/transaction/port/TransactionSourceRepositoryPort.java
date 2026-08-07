package za.co.tinyiko.transactionaggregation.transaction.port;

import java.util.Optional;

import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionSource;

/**
 * Outbound port the transaction application layer uses to look up a transaction source by its
 * code. Read-only, matching {@code categorisation}'s established "read-only port needs no save"
 * pattern: nothing in this branch writes a source through application code, only the {@code V10}
 * Flyway seed migration populates {@code transaction_sources}.
 */
public interface TransactionSourceRepositoryPort {

	Optional<TransactionSource> findByCode(String code);

}
