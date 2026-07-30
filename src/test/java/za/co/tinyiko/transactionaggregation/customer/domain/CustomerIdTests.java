package za.co.tinyiko.transactionaggregation.customer.domain;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class CustomerIdTests {

	@Test
	void rejectsNullValue() {
		assertThatNullPointerException().isThrownBy(() -> new CustomerId(null));
	}

	@Test
	void generateProducesDistinctNonNullIds() {
		CustomerId first = CustomerId.generate();
		CustomerId second = CustomerId.generate();

		assertThat(first.value()).isNotNull();
		assertThat(second.value()).isNotNull();
		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void equalityIsByValue() {
		UUID id = UUID.randomUUID();

		assertThat(new CustomerId(id)).isEqualTo(new CustomerId(id));
	}

}
