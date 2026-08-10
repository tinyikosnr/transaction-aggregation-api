package za.co.tinyiko.transactionaggregation.transaction.application;

import java.util.UUID;

/**
 * Thrown by {@link GetTransactionService} when no transaction exists for the given id
 * (SAD 39.4's {@code TRANSACTION_NOT_FOUND}, TDS §65's own documented exception name).
 */
public class TransactionNotFoundException extends RuntimeException {

	public TransactionNotFoundException(UUID transactionId) {
		super("Transaction not found: " + transactionId);
	}

}
