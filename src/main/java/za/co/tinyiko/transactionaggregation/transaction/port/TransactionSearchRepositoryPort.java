package za.co.tinyiko.transactionaggregation.transaction.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for row-level transaction retrieval - get-by-id and search. Deliberately
 * separate from {@link TransactionRepositoryPort} (write + duplicate-check) and
 * {@link TransactionSummaryRepositoryPort} (aggregate SUM/GROUP BY reads for the aggregation
 * module): this port serves a third, genuinely distinct concern, individual-row retrieval, with
 * its own purpose-built Spring Data interface and adapter behind it - the same reasoning that
 * already justified splitting {@code TransactionSummaryRepositoryPort} out on its own.
 */
public interface TransactionSearchRepositoryPort {

	Optional<TransactionDetailRow> findDetailById(UUID transactionId);

	TransactionSearchPage search(TransactionSearchQuery query);

}
