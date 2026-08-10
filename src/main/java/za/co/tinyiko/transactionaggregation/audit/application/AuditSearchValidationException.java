package za.co.tinyiko.transactionaggregation.audit.application;

/**
 * Thrown by {@link SearchAuditEventsService} when search criteria fail structural validation
 * (invalid page/size, unsupported sort field/direction, {@code occurredFrom} after
 * {@code occurredTo}, or {@code aggregateId} supplied without {@code aggregateType}) - the same
 * translation role {@code transaction.application.TransactionValidationException} plays for
 * transaction search, reusing the existing, already-catalogued {@code REQUEST_VALIDATION_FAILED}
 * code rather than introducing a new one.
 */
public class AuditSearchValidationException extends RuntimeException {

	public AuditSearchValidationException(String message) {
		super(message);
	}

}
