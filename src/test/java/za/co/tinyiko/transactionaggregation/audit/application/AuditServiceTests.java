package za.co.tinyiko.transactionaggregation.audit.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.tinyiko.transactionaggregation.audit.domain.AuditEvent;
import za.co.tinyiko.transactionaggregation.audit.port.AuditRepositoryPort;
import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-31T08:15:00Z"), ZoneOffset.UTC);
	private static final CorrelationId A_CORRELATION_ID = new CorrelationId("d91b836b-6137-4c8c-a7ec-534ac69f4680");

	@Mock
	private AuditRepositoryPort auditRepositoryPort;

	@Test
	void buildsAndSavesAnAuditEventFromTheCommand() {
		UUID aggregateId = UUID.randomUUID();
		RecordAuditEventCommand command = new RecordAuditEventCommand(
				"TRANSACTION", aggregateId, "TRANSACTION_CREATED", "api-consumer-1", A_CORRELATION_ID, "{}");

		AuditService service = new AuditService(auditRepositoryPort, FIXED_CLOCK);
		service.record(command);

		ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
		verify(auditRepositoryPort).save(captor.capture());
		AuditEvent recorded = captor.getValue();
		assertThat(recorded.aggregateType()).isEqualTo("TRANSACTION");
		assertThat(recorded.aggregateId()).isEqualTo(aggregateId);
		assertThat(recorded.eventType()).isEqualTo("TRANSACTION_CREATED");
		assertThat(recorded.actor()).isEqualTo("api-consumer-1");
		assertThat(recorded.correlationId()).isEqualTo(A_CORRELATION_ID);
		assertThat(recorded.eventData()).isEqualTo("{}");
		assertThat(recorded.occurredAt()).isEqualTo(Instant.parse("2026-07-31T08:15:00Z"));
	}

	@Test
	void generatesADistinctIdForEachRecordedEvent() {
		RecordAuditEventCommand command = new RecordAuditEventCommand(
				"TRANSACTION", UUID.randomUUID(), "TRANSACTION_CREATED", "actor", A_CORRELATION_ID, "{}");

		AuditService service = new AuditService(auditRepositoryPort, FIXED_CLOCK);
		service.record(command);
		service.record(command);

		ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
		verify(auditRepositoryPort, times(2)).save(captor.capture());
		assertThat(captor.getAllValues().get(0).id()).isNotEqualTo(captor.getAllValues().get(1).id());
	}

	@Test
	void delegatesPersistenceToThePort() {
		RecordAuditEventCommand command = new RecordAuditEventCommand(
				"TRANSACTION", UUID.randomUUID(), "TRANSACTION_CREATED", "actor", A_CORRELATION_ID, "{}");

		AuditService service = new AuditService(auditRepositoryPort, FIXED_CLOCK);
		service.record(command);

		ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
		verify(auditRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().eventType()).isEqualTo("TRANSACTION_CREATED");
	}

}
