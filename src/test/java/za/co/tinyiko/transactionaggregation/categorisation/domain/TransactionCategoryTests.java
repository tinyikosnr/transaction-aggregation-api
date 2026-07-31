package za.co.tinyiko.transactionaggregation.categorisation.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class TransactionCategoryTests {

	private static final Instant FIXED_INSTANT = Instant.parse("2026-07-31T08:15:00Z");
	private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

	private static TransactionCategoryId anId() {
		return TransactionCategoryId.generate();
	}

	@Test
	void registerCreatesCategoryWithTimestampsAndActiveTrue() {
		TransactionCategoryId id = anId();

		TransactionCategory category = TransactionCategory.register(id, "GROCERIES", "Groceries", "Food and household", false, FIXED_CLOCK);

		assertThat(category.id()).isEqualTo(id);
		assertThat(category.code()).isEqualTo("GROCERIES");
		assertThat(category.name()).isEqualTo("Groceries");
		assertThat(category.description()).isEqualTo("Food and household");
		assertThat(category.fallback()).isFalse();
		assertThat(category.active()).isTrue();
		assertThat(category.createdAt()).isEqualTo(FIXED_INSTANT);
		assertThat(category.updatedAt()).isEqualTo(FIXED_INSTANT);
	}

	@Test
	void registerAllowsNullDescription() {
		TransactionCategory category = TransactionCategory.register(anId(), "GROCERIES", "Groceries", null, false, FIXED_CLOCK);

		assertThat(category.description()).isNull();
	}

	@Test
	void registerRejectsNullId() {
		assertThatNullPointerException()
				.isThrownBy(() -> TransactionCategory.register(null, "GROCERIES", "Groceries", null, false, FIXED_CLOCK));
	}

	@Test
	void registerRejectsBlankCode() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> TransactionCategory.register(anId(), " ", "Groceries", null, false, FIXED_CLOCK));
	}

	@Test
	void registerRejectsBlankName() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> TransactionCategory.register(anId(), "GROCERIES", " ", null, false, FIXED_CLOCK));
	}

	@Test
	void registerRejectsCodeTooLong() {
		String tooLong = "X".repeat(51);

		assertThatIllegalArgumentException()
				.isThrownBy(() -> TransactionCategory.register(anId(), tooLong, "Groceries", null, false, FIXED_CLOCK));
	}

	@Test
	void registerRejectsNameTooLong() {
		String tooLong = "X".repeat(101);

		assertThatIllegalArgumentException()
				.isThrownBy(() -> TransactionCategory.register(anId(), "GROCERIES", tooLong, null, false, FIXED_CLOCK));
	}

	@Test
	void registerRejectsDescriptionTooLong() {
		String tooLong = "X".repeat(501);

		assertThatIllegalArgumentException()
				.isThrownBy(() -> TransactionCategory.register(anId(), "GROCERIES", "Groceries", tooLong, false, FIXED_CLOCK));
	}

	@Test
	void equalityIsByIdOnly() {
		TransactionCategoryId id = anId();
		TransactionCategory first = TransactionCategory.register(id, "GROCERIES", "Groceries", null, false, FIXED_CLOCK);
		TransactionCategory second = TransactionCategory.reconstitute(id, "DIFFERENT", "Different", null, false, true, FIXED_INSTANT, FIXED_INSTANT);
		TransactionCategory differentId = TransactionCategory.register(anId(), "GROCERIES", "Groceries", null, false, FIXED_CLOCK);

		assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
		assertThat(first).isNotEqualTo(differentId);
	}

	@Test
	void reconstituteRebuildsWithoutRunningRegistrationInvariants() {
		TransactionCategoryId id = anId();

		TransactionCategory category = TransactionCategory.reconstitute(id, "UNCATEGORISED", "Uncategorised", null, true, false, FIXED_INSTANT, FIXED_INSTANT);

		assertThat(category.fallback()).isTrue();
		assertThat(category.active()).isFalse();
	}

	@Test
	void hasNoMutationMethods() {
		assertThat(TransactionCategory.class.getDeclaredMethods())
				.extracting(java.lang.reflect.Method::getName)
				.doesNotContain("activate", "deactivate", "setName", "setDescription");
	}

}
