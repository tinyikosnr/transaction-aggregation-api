package za.co.tinyiko.transactionaggregation.transaction.domain;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class MoneyTests {

	@Test
	void rejectsNullAmount() {
		assertThatNullPointerException().isThrownBy(() -> new Money(null, "ZAR"));
	}

	@Test
	void rejectsNullCurrency() {
		assertThatNullPointerException().isThrownBy(() -> new Money(new BigDecimal("10.00"), null));
	}

	@Test
	void rejectsNonIsoShapedCurrency() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Money(new BigDecimal("10.00"), "zar"));
		assertThatIllegalArgumentException().isThrownBy(() -> new Money(new BigDecimal("10.00"), "ZA"));
		assertThatIllegalArgumentException().isThrownBy(() -> new Money(new BigDecimal("10.00"), "ZARR"));
	}

	@Test
	void rejectsMoreThanTwoDecimalPlacesOfRealPrecision() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Money(new BigDecimal("10.123"), "ZAR"));
	}

	@Test
	void normalisesScaleSoEquivalentValuesAreEqual() {
		// The database column is NUMERIC(19,4): PostgreSQL always returns values padded to
		// scale 4. A Money built directly at scale 2 must equal one reconstructed from a
		// scale-4 value with only zero padding beyond the second decimal place.
		Money fromTwoDecimalPlaces = new Money(new BigDecimal("125.50"), "ZAR");
		Money fromFourDecimalPlaces = new Money(new BigDecimal("125.5000"), "ZAR");
		Money fromNoDecimalPlaces = new Money(new BigDecimal("125"), "ZAR");

		assertThat(fromTwoDecimalPlaces).isEqualTo(fromFourDecimalPlaces);
		assertThat(fromTwoDecimalPlaces.amount()).isEqualByComparingTo("125.50");
		assertThat(fromNoDecimalPlaces.amount()).isEqualByComparingTo("125.00");
	}

	@Test
	void constructsSuccessfullyWithValidInput() {
		Money money = new Money(new BigDecimal("1250.50"), "ZAR");

		assertThat(money.amount()).isEqualByComparingTo("1250.50");
		assertThat(money.currency()).isEqualTo("ZAR");
	}

}
