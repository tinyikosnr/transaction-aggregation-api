package za.co.tinyiko.transactionaggregation.audit.application;

import java.time.Instant;
import java.util.UUID;

/**
 * The read shape of an audit event (feature/audit-query) - deliberately distinct from the domain
 * {@link za.co.tinyiko.transactionaggregation.audit.domain.AuditEvent}, the append-only write
 * aggregate. Keeping the query API decoupled from that aggregate's own shape means a future
 * write-side change has no forced ripple into the read side, and vice versa. {@code eventData}
 * stays a raw JSON {@code String} here too - parsed into structured JSON only at the API mapping
 * boundary ({@code api.mapper.AuditEventApiMapper}), never in this module.
 */
public record AuditEventView(
		UUID id,
		String aggregateType,
		UUID aggregateId,
		String eventType,
		String actor,
		String correlationId,
		String eventData,
		Instant occurredAt
) {
}
