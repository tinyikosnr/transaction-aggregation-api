package za.co.tinyiko.transactionaggregation.audit.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;

/**
 * The AuditEvent aggregate (SAD 27.7). Following SAD's shape over TDS's: SAD's
 * {@code eventData}/{@code occurredAt} field names are kept over TDS's {@code details}/
 * {@code createdAt}, and TDS's {@code outcome} field (an {@code AuditOutcome} enum) is dropped
 * entirely - SAD's own schema for {@code audit_events} has no such column at all. Success versus
 * failure is expected to be distinguishable via {@code eventType} naming (e.g.
 * {@code TRANSACTION_CREATED} vs {@code TRANSACTION_DUPLICATE_REJECTED}), matching how TDS's own
 * flow diagrams (51, 52) already differentiate the two cases as different named steps, not a
 * shared field.
 *
 * <p>{@code aggregateId} is a raw {@code UUID}, not a typed id: SAD 30.1 notes audit records may
 * refer to different aggregate types, with referential meaning maintained by the
 * {@code aggregateType}/{@code aggregateId} pair rather than a single typed foreign key.
 *
 * <p>Immutable and append-only (SAD 27.7's invariants: audit events are never updated after
 * creation). No {@code @Version}: SAD's own {@code audit_events} DDL has no version column
 * either, consistent with there being no update path to protect against lost updates.
 */
public final class AuditEvent {

	private static final int MAX_AGGREGATE_TYPE_LENGTH = 100;
	private static final int MAX_EVENT_TYPE_LENGTH = 100;
	private static final int MAX_ACTOR_LENGTH = 150;
	private static final int MAX_CORRELATION_ID_LENGTH = 100;

	private final AuditEventId id;
	private final String aggregateType;
	private final UUID aggregateId;
	private final String eventType;
	private final String actor;
	private final CorrelationId correlationId;
	private final String eventData;
	private final Instant occurredAt;

	private AuditEvent(
			AuditEventId id,
			String aggregateType,
			UUID aggregateId,
			String eventType,
			String actor,
			CorrelationId correlationId,
			String eventData,
			Instant occurredAt
	) {
		this.id = id;
		this.aggregateType = aggregateType;
		this.aggregateId = aggregateId;
		this.eventType = eventType;
		this.actor = actor;
		this.correlationId = correlationId;
		this.eventData = eventData;
		this.occurredAt = occurredAt;
	}

	public static AuditEvent register(
			AuditEventId id,
			String aggregateType,
			UUID aggregateId,
			String eventType,
			String actor,
			CorrelationId correlationId,
			String eventData,
			Clock clock
	) {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(aggregateId, "aggregateId must not be null");
		Objects.requireNonNull(correlationId, "correlationId must not be null");
		Objects.requireNonNull(eventData, "eventData must not be null");
		Objects.requireNonNull(clock, "clock must not be null");
		String validatedAggregateType = requireInBounds(aggregateType, "aggregateType", MAX_AGGREGATE_TYPE_LENGTH);
		String validatedEventType = requireInBounds(eventType, "eventType", MAX_EVENT_TYPE_LENGTH);
		String validatedActor = requireInBounds(actor, "actor", MAX_ACTOR_LENGTH);
		if (correlationId.value().length() > MAX_CORRELATION_ID_LENGTH) {
			throw new IllegalArgumentException("correlationId must not exceed " + MAX_CORRELATION_ID_LENGTH + " characters");
		}

		return new AuditEvent(id, validatedAggregateType, aggregateId, validatedEventType, validatedActor,
				correlationId, eventData, Instant.now(clock));
	}

	public static AuditEvent reconstitute(
			AuditEventId id,
			String aggregateType,
			UUID aggregateId,
			String eventType,
			String actor,
			CorrelationId correlationId,
			String eventData,
			Instant occurredAt
	) {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(aggregateType, "aggregateType must not be null");
		Objects.requireNonNull(aggregateId, "aggregateId must not be null");
		Objects.requireNonNull(eventType, "eventType must not be null");
		Objects.requireNonNull(actor, "actor must not be null");
		Objects.requireNonNull(correlationId, "correlationId must not be null");
		Objects.requireNonNull(eventData, "eventData must not be null");
		Objects.requireNonNull(occurredAt, "occurredAt must not be null");
		return new AuditEvent(id, aggregateType, aggregateId, eventType, actor, correlationId, eventData, occurredAt);
	}

	private static String requireInBounds(String value, String fieldName, int maxLength) {
		Objects.requireNonNull(value, fieldName + " must not be null");
		if (value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		if (value.length() > maxLength) {
			throw new IllegalArgumentException(fieldName + " must not exceed " + maxLength + " characters");
		}
		return value;
	}

	public AuditEventId id() {
		return id;
	}

	public String aggregateType() {
		return aggregateType;
	}

	public UUID aggregateId() {
		return aggregateId;
	}

	public String eventType() {
		return eventType;
	}

	public String actor() {
		return actor;
	}

	public CorrelationId correlationId() {
		return correlationId;
	}

	public String eventData() {
		return eventData;
	}

	public Instant occurredAt() {
		return occurredAt;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AuditEvent otherEvent)) {
			return false;
		}
		return id.equals(otherEvent.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return "AuditEvent{id=%s, aggregateType=%s, eventType=%s}".formatted(id, aggregateType, eventType);
	}

}
