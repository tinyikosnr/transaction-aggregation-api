package za.co.tinyiko.transactionaggregation.transaction.domain;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class TransactionIdTests {

	@Test
	void rejectsNullValue() {
		assertThatNullPointerException().isThrownBy(() -> new TransactionId(null));
	}

	@Test
	void generateProducesDistinctNonNullIds() {
		TransactionId first = TransactionId.generate();
		TransactionId second = TransactionId.generate();

		assertThat(first.value()).isNotNull();
		assertThat(second.value()).isNotNull();
		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void equalityIsByValue() {
		UUID id = UUID.randomUUID();

		assertThat(new TransactionId(id)).isEqualTo(new TransactionId(id));
	}

}
