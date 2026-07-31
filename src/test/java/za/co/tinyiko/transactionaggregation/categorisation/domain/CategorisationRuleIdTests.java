package za.co.tinyiko.transactionaggregation.categorisation.domain;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class CategorisationRuleIdTests {

	@Test
	void rejectsNullValue() {
		assertThatNullPointerException().isThrownBy(() -> new CategorisationRuleId(null));
	}

	@Test
	void generateProducesDistinctNonNullIds() {
		CategorisationRuleId first = CategorisationRuleId.generate();
		CategorisationRuleId second = CategorisationRuleId.generate();

		assertThat(first.value()).isNotNull();
		assertThat(second.value()).isNotNull();
		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void equalityIsByValue() {
		UUID id = UUID.randomUUID();

		assertThat(new CategorisationRuleId(id)).isEqualTo(new CategorisationRuleId(id));
	}

}
