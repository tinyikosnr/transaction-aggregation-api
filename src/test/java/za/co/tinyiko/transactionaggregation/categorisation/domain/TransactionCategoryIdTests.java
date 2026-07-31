package za.co.tinyiko.transactionaggregation.categorisation.domain;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class TransactionCategoryIdTests {

	@Test
	void rejectsNullValue() {
		assertThatNullPointerException().isThrownBy(() -> new TransactionCategoryId(null));
	}

	@Test
	void generateProducesDistinctNonNullIds() {
		TransactionCategoryId first = TransactionCategoryId.generate();
		TransactionCategoryId second = TransactionCategoryId.generate();

		assertThat(first.value()).isNotNull();
		assertThat(second.value()).isNotNull();
		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void equalityIsByValue() {
		UUID id = UUID.randomUUID();

		assertThat(new TransactionCategoryId(id)).isEqualTo(new TransactionCategoryId(id));
	}

}
