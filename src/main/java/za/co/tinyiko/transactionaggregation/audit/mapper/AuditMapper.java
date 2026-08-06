package za.co.tinyiko.transactionaggregation.audit.mapper;

import za.co.tinyiko.transactionaggregation.audit.domain.AuditEvent;
import za.co.tinyiko.transactionaggregation.audit.domain.AuditEventId;
import za.co.tinyiko.transactionaggregation.audit.persistence.AuditEventEntity;
import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;

/**
 * Maps between the domain {@link AuditEvent} and the JPA {@link AuditEventEntity}.
 *
 * <p>{@code toEntity}, not {@code applyTo}: audit events are never looked up and mutated in
 * place (SAD 27.7's append-only invariant means there is no existing managed entity to update),
 * so the mapper builds a fresh, fully-formed {@link AuditEventEntity} via its all-args
 * constructor rather than mutating one passed in.
 */
public final class AuditMapper {

	private AuditMapper() {
	}

	public static AuditEvent toDomain(AuditEventEntity entity) {
		return AuditEvent.reconstitute(
				new AuditEventId(entity.getId()),
				entity.getAggregateType(),
				entity.getAggregateId(),
				entity.getEventType(),
				entity.getActor(),
				new CorrelationId(entity.getCorrelationId()),
				entity.getEventData(),
				entity.getOccurredAt());
	}

	public static AuditEventEntity toEntity(AuditEvent auditEvent) {
		return new AuditEventEntity(
				auditEvent.id().value(),
				auditEvent.aggregateType(),
				auditEvent.aggregateId(),
				auditEvent.eventType(),
				auditEvent.actor(),
				auditEvent.correlationId().value(),
				auditEvent.eventData(),
				auditEvent.occurredAt());
	}

}
