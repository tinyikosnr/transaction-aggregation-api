package za.co.tinyiko.transactionaggregation.transaction.application;

/**
 * Inbound port for creating a single transaction (TDS 4). The only capability this branch
 * implements - bulk creation, retrieval and search are documented but deferred (TDS 70's own
 * "recommended first implementation slice" scopes to single creation only).
 */
public interface CreateTransactionUseCase {

	TransactionCreatedResult create(CreateTransactionCommand command);

}
