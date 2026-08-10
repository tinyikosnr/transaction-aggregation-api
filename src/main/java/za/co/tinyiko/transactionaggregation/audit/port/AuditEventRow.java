package za.co.tinyiko.transactionaggregation.audit.port;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code audit.port}'s own row shape for {@link AuditQueryRepositoryPort#search}, deliberately
 * distinct from the domain {@link za.co.tinyiko.transactionaggregation.audit.domain.AuditEvent} -
 * the same "port owns its own shape" rule already established by {@code transaction.port}'s
 * {@code TransactionSearchRow}. {@code correlationId} is a plain {@code String}, not
 * {@code shared.logging.CorrelationId}: this is a read projection, not a write aggregate, and has
 * no caller-supplied {@code CorrelationId} instance to preserve. {@code eventData} stays a raw
 * JSON {@code String} - never parsed below the API mapping boundary (feature/audit-query).
 */
public record AuditEventRow(
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
