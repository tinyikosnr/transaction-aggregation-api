package za.co.tinyiko.transactionaggregation.shared.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The common envelope every module's domain events are published in, exactly as specified in
 * the Technical Design Specification (Part 10, §45).
 *
 * <p>{@code payload} is deliberately unconstrained ({@code T}, not {@code T extends DomainEvent}
 * or similar) — this is a considered choice, not an oversight, to keep the envelope matching the
 * approved TDS shape exactly. No module currently publishes an event, and this type has no
 * {@code DomainEventPublisher} to pair with yet; that port is deferred until the first module
 * actually needs to publish something.
 */
public record DomainEventEnvelope<T>(
		UUID eventId,
		String eventType,
		Instant occurredAt,
		String correlationId,
		T payload
) {

	public DomainEventEnvelope {
		Objects.requireNonNull(eventId, "eventId must not be null");
		Objects.requireNonNull(eventType, "eventType must not be null");
		Objects.requireNonNull(occurredAt, "occurredAt must not be null");
		Objects.requireNonNull(correlationId, "correlationId must not be null");
		Objects.requireNonNull(payload, "payload must not be null");
	}

}
