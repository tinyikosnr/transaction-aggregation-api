package za.co.tinyiko.transactionaggregation.audit.application;

import za.co.tinyiko.transactionaggregation.audit.domain.AuditEvent;

/**
 * Inbound port other modules use to record an audit event (TDS 8, SAD 37.5). Named for the
 * verb, not {@code AuditEventPublisher}: TDS 8's sketch of that name implies event-bus/publish
 * semantics this isn't - it's a direct synchronous call, the same shape as
 * {@code CategoriseTransactionUseCase}, not a fire-and-forget publication. TDS's own later,
 * more authoritative class catalogue (57-63) doesn't mention {@code AuditEventPublisher} or
 * {@code AuditRecord} at all.
 */
public interface RecordAuditEventUseCase {

	AuditEvent record(RecordAuditEventCommand command);

}
