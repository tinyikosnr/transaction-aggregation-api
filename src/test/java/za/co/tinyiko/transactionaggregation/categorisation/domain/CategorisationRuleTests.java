package za.co.tinyiko.transactionaggregation.categorisation.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class CategorisationRuleTests {

	private static final Instant FIXED_INSTANT = Instant.parse("2026-07-31T08:15:00Z");
	private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

	private static CategorisationRuleId anId() {
		return CategorisationRuleId.generate();
	}

	private static TransactionCategoryId aCategoryId() {
		return TransactionCategoryId.generate();
	}

	@Test
	void registerCreatesRuleWithUppercasedMatchValueAndActiveTrue() {
		CategorisationRuleId id = anId();
		TransactionCategoryId categoryId = aCategoryId();

		CategorisationRule rule = CategorisationRule.register(
				id, categoryId, MatchField.MERCHANT, MatchOperator.CONTAINS, "checkers", Direction.DEBIT, 20, FIXED_CLOCK);

		assertThat(rule.id()).isEqualTo(id);
		assertThat(rule.categoryId()).isEqualTo(categoryId);
		assertThat(rule.matchField()).isEqualTo(MatchField.MERCHANT);
		assertThat(rule.operator()).isEqualTo(MatchOperator.CONTAINS);
		assertThat(rule.matchValue()).isEqualTo("CHECKERS");
		assertThat(rule.direction()).isEqualTo(Direction.DEBIT);
		assertThat(rule.priority()).isEqualTo(20);
		assertThat(rule.active()).isTrue();
		assertThat(rule.createdAt()).isEqualTo(FIXED_INSTANT);
		assertThat(rule.updatedAt()).isEqualTo(FIXED_INSTANT);
	}

	@Test
	void registerRejectsNullId() {
		assertThatNullPointerException().isThrownBy(() -> CategorisationRule.register(
				null, aCategoryId(), MatchField.MERCHANT, MatchOperator.CONTAINS, "SHELL", Direction.DEBIT, 10, FIXED_CLOCK));
	}

	@Test
	void registerRejectsNullDirection() {
		assertThatNullPointerException().isThrownBy(() -> CategorisationRule.register(
				anId(), aCategoryId(), MatchField.MERCHANT, MatchOperator.CONTAINS, "SHELL", null, 10, FIXED_CLOCK));
	}

	@Test
	void registerRejectsBlankMatchValue() {
		assertThatIllegalArgumentException().isThrownBy(() -> CategorisationRule.register(
				anId(), aCategoryId(), MatchField.MERCHANT, MatchOperator.CONTAINS, " ", Direction.DEBIT, 10, FIXED_CLOCK));
	}

	@Test
	void registerRejectsMatchValueTooLong() {
		String tooLong = "X".repeat(201);

		assertThatIllegalArgumentException().isThrownBy(() -> CategorisationRule.register(
				anId(), aCategoryId(), MatchField.MERCHANT, MatchOperator.CONTAINS, tooLong, Direction.DEBIT, 10, FIXED_CLOCK));
	}

	@Test
	void registerRejectsNonPositivePriority() {
		assertThatIllegalArgumentException().isThrownBy(() -> CategorisationRule.register(
				anId(), aCategoryId(), MatchField.MERCHANT, MatchOperator.CONTAINS, "SHELL", Direction.DEBIT, 0, FIXED_CLOCK));
	}

	@Test
	void equalityIsByIdOnly() {
		CategorisationRuleId id = anId();
		CategorisationRule first = CategorisationRule.register(
				id, aCategoryId(), MatchField.MERCHANT, MatchOperator.CONTAINS, "SHELL", Direction.DEBIT, 10, FIXED_CLOCK);
		CategorisationRule second = CategorisationRule.reconstitute(
				id, aCategoryId(), MatchField.DESCRIPTION, MatchOperator.EQUALS, "OTHER", Direction.CREDIT, 20, false, FIXED_INSTANT, FIXED_INSTANT);

		assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
	}

	@Test
	void matchesReturnsFalseWhenDirectionDiffers() {
		CategorisationRule rule = CategorisationRule.register(
				anId(), aCategoryId(), MatchField.MERCHANT, MatchOperator.CONTAINS, "SHELL", Direction.DEBIT, 10, FIXED_CLOCK);

		assertThat(rule.matches("SHELL GARAGE", "fuel purchase", Direction.CREDIT)).isFalse();
	}

	@Test
	void matchesReturnsFalseWhenFieldTextIsNull() {
		CategorisationRule rule = CategorisationRule.register(
				anId(), aCategoryId(), MatchField.MERCHANT, MatchOperator.CONTAINS, "SHELL", Direction.DEBIT, 10, FIXED_CLOCK);

		assertThat(rule.matches(null, "fuel purchase", Direction.DEBIT)).isFalse();
	}

	@Test
	void matchesEqualsIsCaseInsensitiveAndExact() {
		CategorisationRule rule = CategorisationRule.register(
				anId(), aCategoryId(), MatchField.MERCHANT, MatchOperator.EQUALS, "shell", Direction.DEBIT, 10, FIXED_CLOCK);

		assertThat(rule.matches("Shell", "desc", Direction.DEBIT)).isTrue();
		assertThat(rule.matches("Shell Garage", "desc", Direction.DEBIT)).isFalse();
	}

	@Test
	void matchesContainsIsCaseInsensitiveSubstring() {
		CategorisationRule rule = CategorisationRule.register(
				anId(), aCategoryId(), MatchField.MERCHANT, MatchOperator.CONTAINS, "shell", Direction.DEBIT, 10, FIXED_CLOCK);

		assertThat(rule.matches("Shell Garage Centurion", "desc", Direction.DEBIT)).isTrue();
		assertThat(rule.matches("BP Garage", "desc", Direction.DEBIT)).isFalse();
	}

	@Test
	void matchesRegexAppliesAgainstTheConfiguredField() {
		CategorisationRule rule = CategorisationRule.register(
				anId(), aCategoryId(), MatchField.DESCRIPTION, MatchOperator.REGEX, ".*", Direction.DEBIT, 1000, FIXED_CLOCK);

		assertThat(rule.matches(null, "anything at all", Direction.DEBIT)).isTrue();
	}

	@Test
	void matchesUsesDescriptionWhenMatchFieldIsDescription() {
		CategorisationRule rule = CategorisationRule.register(
				anId(), aCategoryId(), MatchField.DESCRIPTION, MatchOperator.CONTAINS, "salary", Direction.CREDIT, 10, FIXED_CLOCK);

		assertThat(rule.matches("ACME PAYROLL", "Monthly Salary payment", Direction.CREDIT)).isTrue();
		assertThat(rule.matches("Salary Inc", "Monthly transfer", Direction.CREDIT)).isFalse();
	}

	@Test
	void matchesIgnoresActiveFlag() {
		CategorisationRule rule = CategorisationRule.reconstitute(
				anId(), aCategoryId(), MatchField.MERCHANT, MatchOperator.EQUALS, "SHELL", Direction.DEBIT, 10, false, FIXED_INSTANT, FIXED_INSTANT);

		assertThat(rule.matches("SHELL", "desc", Direction.DEBIT)).isTrue();
	}

}
