package za.co.tinyiko.transactionaggregation.audit.application;

import java.time.Instant;
import java.util.UUID;

/**
 * The raw, caller-supplied audit search criteria (feature/audit-query) - not yet normalised or
 * validated; {@code SearchAuditEventsService} owns that. String filters may be {@code null},
 * empty, or blank on entry; {@code page}/{@code size} are always required, {@code sort} is the
 * raw, unparsed query string (defaulting to {@code occurredAt,desc} when absent).
 */
public record AuditEventSearchCriteria(
		String aggregateType,
		UUID aggregateId,
		String eventType,
		String actor,
		String correlationId,
		Instant occurredFrom,
		Instant occurredTo,
		int page,
		int size,
		String sort
) {
}
