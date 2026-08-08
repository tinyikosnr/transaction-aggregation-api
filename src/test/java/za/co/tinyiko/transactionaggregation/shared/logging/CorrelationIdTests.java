package za.co.tinyiko.transactionaggregation.shared.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class CorrelationIdTests {

	@Test
	void retainsSuppliedValue() {
		var correlationId = new CorrelationId("f68017fa-2f98-4a52-a356-21fe41004965");

		assertThat(correlationId.value()).isEqualTo("f68017fa-2f98-4a52-a356-21fe41004965");
	}

	@Test
	void rejectsNullValue() {
		assertThatNullPointerException().isThrownBy(() -> new CorrelationId(null));
	}

	@Test
	void rejectsBlankValue() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CorrelationId("   "));
	}

	@Test
	void equalityIsByValue() {
		var first = new CorrelationId("same-value");
		var second = new CorrelationId("same-value");

		assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
	}

	@Test
	void exposesTheStandardHeaderName() {
		assertThat(CorrelationId.HEADER_NAME).isEqualTo("X-Correlation-ID");
	}

	@Test
	void generateProducesAValidNonBlankValue() {
		CorrelationId generated = CorrelationId.generate();

		assertThat(generated.value()).isNotBlank();
	}

	@Test
	void generateProducesDistinctValuesOnEachCall() {
		CorrelationId first = CorrelationId.generate();
		CorrelationId second = CorrelationId.generate();

		assertThat(first).isNotEqualTo(second);
	}

}
