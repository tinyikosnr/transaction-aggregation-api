package za.co.tinyiko.transactionaggregation.transaction.application;

import java.util.UUID;

/**
 * Thrown by {@link CreateTransactionService} when a transaction with the same source and
 * external transaction id already exists (BR-07/BR-08, TRX-001) - whether caught by the
 * application-level pre-check or by the database's unique-constraint as the final
 * concurrency-safe guard. Unlike {@code merchant.application.DuplicateMerchantException},
 * there is no recovery-by-returning-the-existing-row here: BR-08 requires duplicates to be
 * rejected, not resolved to the existing transaction.
 *
 * <p>Takes the raw {@code transactionSourceId} (not the human-readable source code): that's
 * what's available at both throw sites (the application-level pre-check and the persistence
 * adapter's race-condition catch), whereas the source code is only known at the former.
 */
public class DuplicateTransactionException extends RuntimeException {

	public DuplicateTransactionException(UUID transactionSourceId, String externalTransactionId) {
		super("A transaction from source '" + transactionSourceId + "' with external id '" + externalTransactionId
				+ "' already exists");
	}

	public DuplicateTransactionException(UUID transactionSourceId, String externalTransactionId, Throwable cause) {
		super("A transaction from source '" + transactionSourceId + "' with external id '" + externalTransactionId
				+ "' already exists", cause);
	}

}
