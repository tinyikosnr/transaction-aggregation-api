package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.time.Instant;
import java.util.UUID;

import tools.jackson.databind.JsonNode;

/**
 * One audit event, as returned by {@code GET /api/v1/audit-events} (feature/audit-query).
 * {@code eventData} is typed {@link JsonNode}, not {@code String}: {@code audit_events.event_data}
 * is stored as real JSONB, and the response must embed it as a genuine nested JSON object, not a
 * re-escaped JSON string. The conversion from the raw JSON string {@code audit.application}
 * carries happens exactly once, in {@code api.mapper.AuditEventApiMapper}, using the
 * Boot-autoconfigured {@code ObjectMapper} - never via {@code @JsonRawValue}, and never inside
 * {@code audit.application}/{@code audit.domain}.
 */
public record AuditEventResponse(
		UUID id,
		String aggregateType,
		UUID aggregateId,
		String eventType,
		String actor,
		String correlationId,
		JsonNode eventData,
		Instant occurredAt
) {
}
