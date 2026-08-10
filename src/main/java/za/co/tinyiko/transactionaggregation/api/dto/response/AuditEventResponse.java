package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.time.Instant;
import java.util.UUID;

import tools.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One audit event, as returned by {@code GET /api/v1/audit-events} (feature/audit-query).
 * {@code eventData} is typed {@link JsonNode}, not {@code String}: {@code audit_events.event_data}
 * is stored as real JSONB, and the response must embed it as a genuine nested JSON object, not a
 * re-escaped JSON string. The conversion from the raw JSON string {@code audit.application}
 * carries happens exactly once, in {@code api.mapper.AuditEventApiMapper}, using the
 * Boot-autoconfigured {@code ObjectMapper} - never via {@code @JsonRawValue}, and never inside
 * {@code audit.application}/{@code audit.domain}.
 */
@Schema(description = "One recorded audit event (append-only, never updated).")
public record AuditEventResponse(
		@Schema(description = "Audit event identifier.") UUID id,
		@Schema(description = "Type of the aggregate this event is about. Open vocabulary - not restricted to a fixed set of values.",
				example = "TRANSACTION") String aggregateType,
		@Schema(description = "Identifier of the aggregate this event is about.") UUID aggregateId,
		@Schema(description = "The event that occurred. Open vocabulary - not restricted to a fixed set of values.",
				example = "TRANSACTION_CREATED") String eventType,
		@Schema(description = "Identity of the actor that caused this event (JWT subject, or \"system\" for internally-triggered events).",
				example = "api-consumer-1") String actor,
		@Schema(description = "Correlation id of the request that caused this event, for cross-referencing logs/traces.") String correlationId,
		@Schema(description = "Event-specific payload, rendered as a genuine nested JSON object (not a re-escaped string). Shape varies by eventType and is deliberately not fixed by this schema.",
				type = "object", implementation = Object.class) JsonNode eventData,
		@Schema(description = "Timestamp the event occurred, in UTC.") Instant occurredAt
) {
}
