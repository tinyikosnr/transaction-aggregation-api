package za.co.tinyiko.transactionaggregation.audit.mapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.audit.domain.AuditEvent;
import za.co.tinyiko.transactionaggregation.audit.domain.AuditEventId;
import za.co.tinyiko.transactionaggregation.audit.persistence.AuditEventEntity;
import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;

import static org.assertj.core.api.Assertions.assertThat;

class AuditMapperTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-31T08:15:00Z"), ZoneOffset.UTC);
	private static final CorrelationId A_CORRELATION_ID = new CorrelationId("d91b836b-6137-4c8c-a7ec-534ac69f4680");

	@Test
	void toDomainMapsAllFields() {
		UUID id = UUID.randomUUID();
		UUID aggregateId = UUID.randomUUID();
		AuditEventEntity entity = new AuditEventEntity(id, "TRANSACTION", aggregateId, "TRANSACTION_CREATED",
				"api-consumer-1", A_CORRELATION_ID.value(), "{\"amount\":\"10.00\"}", Instant.parse("2026-07-31T08:15:00Z"));

		AuditEvent event = AuditMapper.toDomain(entity);

		assertThat(event.id()).isEqualTo(new AuditEventId(id));
		assertThat(event.aggregateType()).isEqualTo("TRANSACTION");
		assertThat(event.aggregateId()).isEqualTo(aggregateId);
		assertThat(event.eventType()).isEqualTo("TRANSACTION_CREATED");
		assertThat(event.actor()).isEqualTo("api-consumer-1");
		assertThat(event.correlationId()).isEqualTo(A_CORRELATION_ID);
		assertThat(event.eventData()).isEqualTo("{\"amount\":\"10.00\"}");
		assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-07-31T08:15:00Z"));
	}

	@Test
	void toEntityCopiesAllFields() {
		UUID aggregateId = UUID.randomUUID();
		AuditEvent event = AuditEvent.register(AuditEventId.generate(), "TRANSACTION", aggregateId,
				"TRANSACTION_CREATED", "api-consumer-1", A_CORRELATION_ID, "{}", FIXED_CLOCK);

		AuditEventEntity entity = AuditMapper.toEntity(event);

		assertThat(entity.getId()).isEqualTo(event.id().value());
		assertThat(entity.getAggregateType()).isEqualTo("TRANSACTION");
		assertThat(entity.getAggregateId()).isEqualTo(aggregateId);
		assertThat(entity.getEventType()).isEqualTo("TRANSACTION_CREATED");
		assertThat(entity.getActor()).isEqualTo("api-consumer-1");
		assertThat(entity.getCorrelationId()).isEqualTo(A_CORRELATION_ID.value());
		assertThat(entity.getEventData()).isEqualTo("{}");
		assertThat(entity.getOccurredAt()).isEqualTo(event.occurredAt());
	}

}
