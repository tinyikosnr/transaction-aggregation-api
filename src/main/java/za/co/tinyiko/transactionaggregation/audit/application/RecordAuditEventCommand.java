package za.co.tinyiko.transactionaggregation.audit.application;

import java.util.Objects;
import java.util.UUID;

import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;

/**
 * Primitives-only input to {@link RecordAuditEventUseCase}, same shape philosophy as
 * categorisation's {@code CategorisationInput}: keeps this module's inbound port independent
 * of any other module's domain types, so a caller like {@code transaction} depends only on
 * this record's fields, not on {@code audit.domain.AuditEvent} itself.
 *
 * <p>Carries no timestamp - {@link AuditService} generates {@code occurredAt} itself via
 * {@code Clock}, matching every other module's aggregate-creation convention.
 */
public record RecordAuditEventCommand(
		String aggregateType,
		UUID aggregateId,
		String eventType,
		String actor,
		CorrelationId correlationId,
		String eventData
) {

	public RecordAuditEventCommand {
		Objects.requireNonNull(aggregateType, "aggregateType must not be null");
		Objects.requireNonNull(aggregateId, "aggregateId must not be null");
		Objects.requireNonNull(eventType, "eventType must not be null");
		Objects.requireNonNull(actor, "actor must not be null");
		Objects.requireNonNull(correlationId, "correlationId must not be null");
		Objects.requireNonNull(eventData, "eventData must not be null");
	}

}
