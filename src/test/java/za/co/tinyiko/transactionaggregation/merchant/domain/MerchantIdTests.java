package za.co.tinyiko.transactionaggregation.merchant.domain;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class MerchantIdTests {

	@Test
	void rejectsNullValue() {
		assertThatNullPointerException().isThrownBy(() -> new MerchantId(null));
	}

	@Test
	void generateProducesDistinctNonNullIds() {
		MerchantId first = MerchantId.generate();
		MerchantId second = MerchantId.generate();

		assertThat(first.value()).isNotNull();
		assertThat(second.value()).isNotNull();
		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void equalityIsByValue() {
		UUID id = UUID.randomUUID();

		assertThat(new MerchantId(id)).isEqualTo(new MerchantId(id));
	}

}
