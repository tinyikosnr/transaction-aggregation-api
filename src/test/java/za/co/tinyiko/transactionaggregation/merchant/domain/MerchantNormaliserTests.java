package za.co.tinyiko.transactionaggregation.merchant.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantNormaliserTests {

	@Test
	void matchesTheDocumentedWorkedExample() {
		// TDS 38's own example.
		assertThat(MerchantNormaliser.normalise("  Checkers #104 Centurion "))
				.isEqualTo("CHECKERS 104 CENTURION");
	}

	@Test
	void trimsLeadingAndTrailingWhitespace() {
		assertThat(MerchantNormaliser.normalise("  Woolworths  ")).isEqualTo("WOOLWORTHS");
	}

	@Test
	void convertsToUppercase() {
		assertThat(MerchantNormaliser.normalise("Pick n Pay")).isEqualTo("PICK N PAY");
	}

	@Test
	void collapsesRepeatedWhitespace() {
		assertThat(MerchantNormaliser.normalise("Shell    Fuel   Station")).isEqualTo("SHELL FUEL STATION");
	}

	@Test
	void removesPunctuationPaddedBySpacesWithoutLeavingADoubleSpace() {
		assertThat(MerchantNormaliser.normalise("A - B")).isEqualTo("A B");
	}

	@Test
	void alreadyCleanInputIsUnchanged() {
		assertThat(MerchantNormaliser.normalise("SPAR")).isEqualTo("SPAR");
	}

	@Test
	void removesApostrophesAndAmpersands() {
		assertThat(MerchantNormaliser.normalise("Mugg&Bean's")).isEqualTo("MUGGBEANS");
	}

}
