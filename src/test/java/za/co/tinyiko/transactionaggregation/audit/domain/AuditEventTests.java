package za.co.tinyiko.transactionaggregation.audit.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class AuditEventTests {

	private static final Instant FIXED_INSTANT = Instant.parse("2026-07-31T08:15:00Z");
	private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
	private static final CorrelationId A_CORRELATION_ID = new CorrelationId("d91b836b-6137-4c8c-a7ec-534ac69f4680");

	private static AuditEventId anId() {
		return AuditEventId.generate();
	}

	@Test
	void registerCreatesAuditEventWithTimestamp() {
		AuditEventId id = anId();
		UUID aggregateId = UUID.randomUUID();

		AuditEvent event = AuditEvent.register(id, "TRANSACTION", aggregateId, "TRANSACTION_CREATED", "api-consumer-1",
				A_CORRELATION_ID, "{}", FIXED_CLOCK);

		assertThat(event.id()).isEqualTo(id);
		assertThat(event.aggregateType()).isEqualTo("TRANSACTION");
		assertThat(event.aggregateId()).isEqualTo(aggregateId);
		assertThat(event.eventType()).isEqualTo("TRANSACTION_CREATED");
		assertThat(event.actor()).isEqualTo("api-consumer-1");
		assertThat(event.correlationId()).isEqualTo(A_CORRELATION_ID);
		assertThat(event.eventData()).isEqualTo("{}");
		assertThat(event.occurredAt()).isEqualTo(FIXED_INSTANT);
	}

	@Test
	void registerRejectsNullId() {
		assertThatNullPointerException().isThrownBy(() -> AuditEvent.register(
				null, "TRANSACTION", UUID.randomUUID(), "TRANSACTION_CREATED", "actor", A_CORRELATION_ID, "{}", FIXED_CLOCK));
	}

	@Test
	void registerRejectsNullAggregateId() {
		assertThatNullPointerException().isThrownBy(() -> AuditEvent.register(
				anId(), "TRANSACTION", null, "TRANSACTION_CREATED", "actor", A_CORRELATION_ID, "{}", FIXED_CLOCK));
	}

	@Test
	void registerRejectsNullCorrelationId() {
		assertThatNullPointerException().isThrownBy(() -> AuditEvent.register(
				anId(), "TRANSACTION", UUID.randomUUID(), "TRANSACTION_CREATED", "actor", null, "{}", FIXED_CLOCK));
	}

	@Test
	void registerRejectsNullEventData() {
		assertThatNullPointerException().isThrownBy(() -> AuditEvent.register(
				anId(), "TRANSACTION", UUID.randomUUID(), "TRANSACTION_CREATED", "actor", A_CORRELATION_ID, null, FIXED_CLOCK));
	}

	@Test
	void registerRejectsBlankAggregateType() {
		assertThatIllegalArgumentException().isThrownBy(() -> AuditEvent.register(
				anId(), " ", UUID.randomUUID(), "TRANSACTION_CREATED", "actor", A_CORRELATION_ID, "{}", FIXED_CLOCK));
	}

	@Test
	void registerRejectsBlankEventType() {
		assertThatIllegalArgumentException().isThrownBy(() -> AuditEvent.register(
				anId(), "TRANSACTION", UUID.randomUUID(), " ", "actor", A_CORRELATION_ID, "{}", FIXED_CLOCK));
	}

	@Test
	void registerRejectsBlankActor() {
		assertThatIllegalArgumentException().isThrownBy(() -> AuditEvent.register(
				anId(), "TRANSACTION", UUID.randomUUID(), "TRANSACTION_CREATED", " ", A_CORRELATION_ID, "{}", FIXED_CLOCK));
	}

	@Test
	void registerRejectsAggregateTypeTooLong() {
		String tooLong = "X".repeat(101);

		assertThatIllegalArgumentException().isThrownBy(() -> AuditEvent.register(
				anId(), tooLong, UUID.randomUUID(), "TRANSACTION_CREATED", "actor", A_CORRELATION_ID, "{}", FIXED_CLOCK));
	}

	@Test
	void registerRejectsEventTypeTooLong() {
		String tooLong = "X".repeat(101);

		assertThatIllegalArgumentException().isThrownBy(() -> AuditEvent.register(
				anId(), "TRANSACTION", UUID.randomUUID(), tooLong, "actor", A_CORRELATION_ID, "{}", FIXED_CLOCK));
	}

	@Test
	void registerRejectsActorTooLong() {
		String tooLong = "X".repeat(151);

		assertThatIllegalArgumentException().isThrownBy(() -> AuditEvent.register(
				anId(), "TRANSACTION", UUID.randomUUID(), "TRANSACTION_CREATED", tooLong, A_CORRELATION_ID, "{}", FIXED_CLOCK));
	}

	@Test
	void registerRejectsCorrelationIdTooLong() {
		CorrelationId tooLong = new CorrelationId("X".repeat(101));

		assertThatIllegalArgumentException().isThrownBy(() -> AuditEvent.register(
				anId(), "TRANSACTION", UUID.randomUUID(), "TRANSACTION_CREATED", "actor", tooLong, "{}", FIXED_CLOCK));
	}

	@Test
	void equalityIsByIdOnly() {
		AuditEventId id = anId();
		UUID aggregateId = UUID.randomUUID();
		AuditEvent first = AuditEvent.register(id, "TRANSACTION", aggregateId, "TRANSACTION_CREATED", "actor", A_CORRELATION_ID, "{}", FIXED_CLOCK);
		AuditEvent second = AuditEvent.reconstitute(id, "DIFFERENT", UUID.randomUUID(), "DIFFERENT_EVENT", "other",
				A_CORRELATION_ID, "{\"k\":1}", FIXED_INSTANT);

		assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
	}

	@Test
	void reconstituteRebuildsWithoutRunningRegistrationInvariants() {
		AuditEventId id = anId();

		AuditEvent event = AuditEvent.reconstitute(id, "TRANSACTION", UUID.randomUUID(), "TRANSACTION_CREATED", "actor",
				A_CORRELATION_ID, "{}", FIXED_INSTANT);

		assertThat(event.occurredAt()).isEqualTo(FIXED_INSTANT);
	}

	@Test
	void hasNoMutationMethods() {
		assertThat(AuditEvent.class.getDeclaredMethods())
				.extracting(java.lang.reflect.Method::getName)
				.doesNotContain("setEventData", "setActor", "update");
	}

}
