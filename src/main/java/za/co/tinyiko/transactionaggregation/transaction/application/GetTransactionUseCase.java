package za.co.tinyiko.transactionaggregation.transaction.application;

import java.util.UUID;

/**
 * Inbound port for retrieving a single transaction by id (FR-08, UC-03, TDS §30).
 */
public interface GetTransactionUseCase {

	TransactionDetails get(UUID transactionId);

}
