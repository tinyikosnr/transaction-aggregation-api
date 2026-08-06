package za.co.tinyiko.transactionaggregation.audit.domain;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class AuditEventIdTests {

	@Test
	void rejectsNullValue() {
		assertThatNullPointerException().isThrownBy(() -> new AuditEventId(null));
	}

	@Test
	void generateProducesDistinctNonNullIds() {
		AuditEventId first = AuditEventId.generate();
		AuditEventId second = AuditEventId.generate();

		assertThat(first.value()).isNotNull();
		assertThat(second.value()).isNotNull();
		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void equalityIsByValue() {
		UUID id = UUID.randomUUID();

		assertThat(new AuditEventId(id)).isEqualTo(new AuditEventId(id));
	}

}
