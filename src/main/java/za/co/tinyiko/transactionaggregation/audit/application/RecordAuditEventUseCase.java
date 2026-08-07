package za.co.tinyiko.transactionaggregation.audit.application;

/**
 * Inbound port other modules use to record an audit event (TDS 8, SAD 37.5). Named for the
 * verb, not {@code AuditEventPublisher}: TDS 8's sketch of that name implies event-bus/publish
 * semantics this isn't - it's a direct synchronous call, the same shape as
 * {@code CategoriseTransactionUseCase}, not a fire-and-forget publication. TDS's own later,
 * more authoritative class catalogue (57-63) doesn't mention {@code AuditEventPublisher} or
 * {@code AuditRecord} at all.
 *
 * <p>Returns {@code void}, not the domain {@code AuditEvent}: this package is exposed
 * cross-module via {@code @NamedInterface}, but {@code audit.domain} is not, and nothing any
 * caller does is documented as needing the persisted event back - every field a caller might
 * want (id, timestamp) is either already known to the caller or unused by any documented flow.
 * This also matches TDS 8's own original sketch, {@code AuditEventPublisher.publish(...): void}.
 */
public interface RecordAuditEventUseCase {

	void record(RecordAuditEventCommand command);

}
