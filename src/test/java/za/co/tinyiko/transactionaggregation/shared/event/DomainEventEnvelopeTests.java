package za.co.tinyiko.transactionaggregation.shared.event;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DomainEventEnvelopeTests {

	private static final UUID EVENT_ID = UUID.randomUUID();
	private static final String EVENT_TYPE = "sample.event";
	private static final Instant OCCURRED_AT = Instant.parse("2026-07-30T08:15:00Z");
	private static final String CORRELATION_ID = "f68017fa-2f98-4a52-a356-21fe41004965";
	private static final String PAYLOAD = "payload";

	@Test
	void retainsAllSuppliedFields() {
		var envelope = new DomainEventEnvelope<>(EVENT_ID, EVENT_TYPE, OCCURRED_AT, CORRELATION_ID, PAYLOAD);

		assertThat(envelope.eventId()).isEqualTo(EVENT_ID);
		assertThat(envelope.eventType()).isEqualTo(EVENT_TYPE);
		assertThat(envelope.occurredAt()).isEqualTo(OCCURRED_AT);
		assertThat(envelope.correlationId()).isEqualTo(CORRELATION_ID);
		assertThat(envelope.payload()).isEqualTo(PAYLOAD);
	}

	@Test
	void rejectsNullEventId() {
		assertThatNullPointerException()
				.isThrownBy(() -> new DomainEventEnvelope<>(null, EVENT_TYPE, OCCURRED_AT, CORRELATION_ID, PAYLOAD));
	}

	@Test
	void rejectsNullEventType() {
		assertThatNullPointerException()
				.isThrownBy(() -> new DomainEventEnvelope<>(EVENT_ID, null, OCCURRED_AT, CORRELATION_ID, PAYLOAD));
	}

	@Test
	void rejectsNullOccurredAt() {
		assertThatNullPointerException()
				.isThrownBy(() -> new DomainEventEnvelope<>(EVENT_ID, EVENT_TYPE, null, CORRELATION_ID, PAYLOAD));
	}

	@Test
	void rejectsNullCorrelationId() {
		assertThatNullPointerException()
				.isThrownBy(() -> new DomainEventEnvelope<>(EVENT_ID, EVENT_TYPE, OCCURRED_AT, null, PAYLOAD));
	}

	@Test
	void rejectsNullPayload() {
		assertThatNullPointerException()
				.isThrownBy(() -> new DomainEventEnvelope<>(EVENT_ID, EVENT_TYPE, OCCURRED_AT, CORRELATION_ID, null));
	}

	@Test
	void equalityIsByValue() {
		var first = new DomainEventEnvelope<>(EVENT_ID, EVENT_TYPE, OCCURRED_AT, CORRELATION_ID, PAYLOAD);
		var second = new DomainEventEnvelope<>(EVENT_ID, EVENT_TYPE, OCCURRED_AT, CORRELATION_ID, PAYLOAD);

		assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
	}

}
