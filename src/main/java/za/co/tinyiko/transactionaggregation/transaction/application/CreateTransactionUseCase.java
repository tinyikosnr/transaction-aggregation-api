package za.co.tinyiko.transactionaggregation.transaction.application;

/**
 * Inbound port for creating a single transaction (TDS 4). Bulk creation ({@link
 * CreateTransactionsBulkUseCase}, {@code feature/transaction-bulk}) calls this port once per
 * item rather than duplicating its workflow.
 */
public interface CreateTransactionUseCase {

	TransactionCreatedResult create(CreateTransactionCommand command);

}
