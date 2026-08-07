package za.co.tinyiko.transactionaggregation.transaction.application;

/**
 * Thrown by {@link CreateTransactionService} when the requested transaction source code does
 * not exist, or exists but is not {@code ACTIVE} (SAD 27.3: "Only active sources may submit new
 * transactions"). One exception type covers both cases - TDS's error catalogue defines a single
 * code for this (SRC-001), not a separate "found but inactive" code.
 */
public class TransactionSourceNotFoundException extends RuntimeException {

	public TransactionSourceNotFoundException(String sourceCode) {
		super("Transaction source not found or not active: " + sourceCode);
	}

}
