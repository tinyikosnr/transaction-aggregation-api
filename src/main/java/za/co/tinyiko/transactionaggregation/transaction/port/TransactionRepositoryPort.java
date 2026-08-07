package za.co.tinyiko.transactionaggregation.transaction.port;

import java.util.Optional;

import za.co.tinyiko.transactionaggregation.transaction.domain.Transaction;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionSourceId;

/**
 * Outbound port the transaction application layer uses to persist and read transactions
 * (TDS 60). {@code findBySourceIdAndExternalTransactionId} backs the application-level
 * duplicate pre-check (SAD 32.7); {@code save} is the final, concurrency-safe guard via its
 * adapter's unique-constraint translation.
 */
public interface TransactionRepositoryPort {

	Transaction save(Transaction transaction);

	Optional<Transaction> findBySourceIdAndExternalTransactionId(
			TransactionSourceId transactionSourceId, String externalTransactionId);

}
