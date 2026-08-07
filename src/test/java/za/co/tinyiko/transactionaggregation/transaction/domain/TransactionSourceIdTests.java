package za.co.tinyiko.transactionaggregation.transaction.domain;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class TransactionSourceIdTests {

	@Test
	void rejectsNullValue() {
		assertThatNullPointerException().isThrownBy(() -> new TransactionSourceId(null));
	}

	@Test
	void generateProducesDistinctNonNullIds() {
		TransactionSourceId first = TransactionSourceId.generate();
		TransactionSourceId second = TransactionSourceId.generate();

		assertThat(first.value()).isNotNull();
		assertThat(second.value()).isNotNull();
		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void equalityIsByValue() {
		UUID id = UUID.randomUUID();

		assertThat(new TransactionSourceId(id)).isEqualTo(new TransactionSourceId(id));
	}

}
