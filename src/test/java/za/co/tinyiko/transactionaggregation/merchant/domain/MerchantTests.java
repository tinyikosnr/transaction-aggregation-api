package za.co.tinyiko.transactionaggregation.merchant.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class MerchantTests {

	private static final Instant FIXED_INSTANT = Instant.parse("2026-07-31T08:15:00Z");
	private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

	private static MerchantId anId() {
		return MerchantId.generate();
	}

	@Test
	void registerCreatesMerchantWithTimestamps() {
		MerchantId id = anId();

		Merchant merchant = Merchant.register(id, "CHECKERS 104 CENTURION", "Checkers #104 Centurion", FIXED_CLOCK);

		assertThat(merchant.id()).isEqualTo(id);
		assertThat(merchant.normalisedName()).isEqualTo("CHECKERS 104 CENTURION");
		assertThat(merchant.displayName()).isEqualTo("Checkers #104 Centurion");
		assertThat(merchant.createdAt()).isEqualTo(FIXED_INSTANT);
		assertThat(merchant.updatedAt()).isEqualTo(FIXED_INSTANT);
	}

	@Test
	void registerRejectsNullId() {
		assertThatNullPointerException()
				.isThrownBy(() -> Merchant.register(null, "NAME", "Name", FIXED_CLOCK));
	}

	@Test
	void registerRejectsBlankNormalisedName() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Merchant.register(anId(), "  ", "Name", FIXED_CLOCK));
	}

	@Test
	void registerRejectsBlankDisplayName() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Merchant.register(anId(), "NAME", " ", FIXED_CLOCK));
	}

	@Test
	void registerRejectsNormalisedNameTooLong() {
		String tooLong = "X".repeat(256);

		assertThatIllegalArgumentException()
				.isThrownBy(() -> Merchant.register(anId(), tooLong, "Name", FIXED_CLOCK));
	}

	@Test
	void registerRejectsDisplayNameTooLong() {
		String tooLong = "X".repeat(256);

		assertThatIllegalArgumentException()
				.isThrownBy(() -> Merchant.register(anId(), "NAME", tooLong, FIXED_CLOCK));
	}

	@Test
	void equalityIsByIdOnly() {
		MerchantId id = anId();
		Merchant first = Merchant.register(id, "NAME", "Name", FIXED_CLOCK);
		Merchant second = Merchant.reconstitute(id, "DIFFERENT NAME", "Different Name", FIXED_INSTANT, FIXED_INSTANT);
		Merchant differentId = Merchant.register(anId(), "NAME", "Name", FIXED_CLOCK);

		assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
		assertThat(first).isNotEqualTo(differentId);
	}

	@Test
	void reconstituteRebuildsWithoutRunningRegistrationInvariants() {
		MerchantId id = anId();

		Merchant merchant = Merchant.reconstitute(id, "NAME", "Name", FIXED_INSTANT, FIXED_INSTANT);

		assertThat(merchant.normalisedName()).isEqualTo("NAME");
	}

	@Test
	void hasNoMutationMethods() {
		// Documents, rather than merely asserts, that Merchant is immutable after creation:
		// neither SAD nor TDS defines a status or update capability for it.
		assertThat(Merchant.class.getDeclaredMethods())
				.extracting(java.lang.reflect.Method::getName)
				.doesNotContain("activate", "suspend", "deactivate", "setNormalisedName", "setDisplayName");
	}

}
