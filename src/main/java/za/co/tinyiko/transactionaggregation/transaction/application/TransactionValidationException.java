package za.co.tinyiko.transactionaggregation.transaction.application;

/**
 * Thrown by {@link CreateTransactionService} when transaction input fails a structural or
 * business invariant (amount, currency, direction, description, occurrence timestamp - TRX-002,
 * TRX-004, TRX-005). Wraps the {@code IllegalArgumentException}/{@code ArithmeticException}
 * thrown by {@code Money} or {@code Transaction.register} rather than letting those leak
 * directly to a caller, the same translation spirit as the persistence-exception pattern
 * elsewhere in this codebase, applied here to domain-invariant exceptions instead.
 */
public class TransactionValidationException extends RuntimeException {

	public TransactionValidationException(String message, Throwable cause) {
		super(message, cause);
	}

}
