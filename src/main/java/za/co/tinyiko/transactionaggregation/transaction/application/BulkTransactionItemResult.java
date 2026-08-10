package za.co.tinyiko.transactionaggregation.transaction.application;

import java.util.UUID;

/**
 * The outcome of processing one item within a bulk create request (SAD 35.2). Deliberately
 * framework/HTTP neutral: no wire-level "status" word lives here (that vocabulary - CREATED,
 * CONFLICT, FAILED - is a presentation concern derived by {@code api.mapper.TransactionApiMapper}
 * from {@code created}/{@code errorCode}, not something this module needs to know about), the
 * same "no HTTP concepts in transaction.application" discipline every other result type in this
 * module already follows.
 *
 * <p>{@code transactionId} is non-null if and only if {@code created} is {@code true};
 * {@code errorCode}/{@code detail} are non-null if and only if {@code created} is {@code false}.
 * {@code errorCode} reuses the same SAD 39.4 catalogue values {@code api.advice.GlobalExceptionHandler}
 * already maps single-create's exceptions to, so a bulk item's failure reason is never a new,
 * bulk-only code.
 */
public record BulkTransactionItemResult(boolean created, UUID transactionId, String errorCode, String detail) {

	public static BulkTransactionItemResult created(UUID transactionId) {
		return new BulkTransactionItemResult(true, transactionId, null, null);
	}

	public static BulkTransactionItemResult failed(String errorCode, String detail) {
		return new BulkTransactionItemResult(false, null, errorCode, detail);
	}

}
