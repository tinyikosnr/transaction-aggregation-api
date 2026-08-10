package za.co.tinyiko.transactionaggregation.transaction.application;

/**
 * Inbound port for bulk transaction creation (FR-02, UC-02, SAD 35.2/32.5/39.8, TDS 29). Each item
 * is processed independently, in its own transaction (SAD 32.5), so a failure on one item never
 * rolls back another item's already-committed work - see {@link BulkTransactionItemResult}'s own
 * Javadoc for the per-item outcome shape.
 */
public interface CreateTransactionsBulkUseCase {

	BulkTransactionResult create(BulkCreateTransactionsCommand command);

}
