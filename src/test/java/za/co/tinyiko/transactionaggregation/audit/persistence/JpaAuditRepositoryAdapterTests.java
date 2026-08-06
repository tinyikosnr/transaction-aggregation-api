package za.co.tinyiko.transactionaggregation.audit.persistence;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import za.co.tinyiko.transactionaggregation.TestcontainersConfiguration;
import za.co.tinyiko.transactionaggregation.audit.domain.AuditEvent;
import za.co.tinyiko.transactionaggregation.audit.domain.AuditEventId;
import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the port -> adapter -> JPA -> Testcontainers-PostgreSQL chain, including the V7
 * migration Flyway applies at context startup. In particular, verifies that {@code eventData}'s
 * {@code @JdbcTypeCode(SqlTypes.JSON)} mapping round-trips a real structured JSON payload
 * against a real {@code JSONB} column, rather than assuming Hibernate 7's native JSON support
 * behaves as documented.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaAuditRepositoryAdapterTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-31T08:15:00Z"), ZoneOffset.UTC);
	private static final CorrelationId A_CORRELATION_ID = new CorrelationId("d91b836b-6137-4c8c-a7ec-534ac69f4680");

	@Autowired
	private SpringDataAuditRepository springDataAuditRepository;

	private JpaAuditRepositoryAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new JpaAuditRepositoryAdapter(springDataAuditRepository);
	}

	@Test
	void savePersistsAndReturnsTheAuditEvent() {
		UUID aggregateId = UUID.randomUUID();
		AuditEvent event = AuditEvent.register(AuditEventId.generate(), "TRANSACTION", aggregateId,
				"TRANSACTION_CREATED", "api-consumer-1", A_CORRELATION_ID, "{}", FIXED_CLOCK);

		AuditEvent saved = adapter.save(event);

		assertThat(saved.id()).isEqualTo(event.id());
		assertThat(springDataAuditRepository.findById(event.id().value())).isPresent();
	}

	@Test
	void eventDataRoundTripsAsRealJsonbThroughAStructuredPayload() {
		String structuredPayload = "{\"amount\":\"125.50\",\"currency\":\"ZAR\",\"nested\":{\"flag\":true,\"count\":3}}";
		AuditEvent event = AuditEvent.register(AuditEventId.generate(), "TRANSACTION", UUID.randomUUID(),
				"TRANSACTION_CREATED", "api-consumer-1", A_CORRELATION_ID, structuredPayload, FIXED_CLOCK);

		adapter.save(event);

		AuditEventEntity reloaded = springDataAuditRepository.findById(event.id().value()).orElseThrow();
		assertThat(reloaded.getEventData()).isEqualTo(structuredPayload);
	}

	@Test
	void savePersistsAllFieldsIncludingAggregateIdAndCorrelationId() {
		UUID aggregateId = UUID.randomUUID();
		AuditEvent event = AuditEvent.register(AuditEventId.generate(), "CUSTOMER", aggregateId,
				"CUSTOMER_REGISTERED", "system", A_CORRELATION_ID, "{}", FIXED_CLOCK);

		adapter.save(event);

		AuditEventEntity reloaded = springDataAuditRepository.findById(event.id().value()).orElseThrow();
		assertThat(reloaded.getAggregateType()).isEqualTo("CUSTOMER");
		assertThat(reloaded.getAggregateId()).isEqualTo(aggregateId);
		assertThat(reloaded.getEventType()).isEqualTo("CUSTOMER_REGISTERED");
		assertThat(reloaded.getActor()).isEqualTo("system");
		assertThat(reloaded.getCorrelationId()).isEqualTo(A_CORRELATION_ID.value());
		assertThat(reloaded.getOccurredAt()).isEqualTo(Instant.parse("2026-07-31T08:15:00Z"));
	}

}
