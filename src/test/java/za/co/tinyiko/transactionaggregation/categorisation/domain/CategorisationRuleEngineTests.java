package za.co.tinyiko.transactionaggregation.categorisation.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class CategorisationRuleEngineTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-31T08:15:00Z"), ZoneOffset.UTC);

	private static CategorisationRule ruleWithPriority(int priority, MatchOperator operator, String matchValue, Direction direction) {
		return CategorisationRule.register(
				CategorisationRuleId.generate(), TransactionCategoryId.generate(),
				MatchField.MERCHANT, operator, matchValue, direction, priority, FIXED_CLOCK);
	}

	@Test
	void selectsTheLowestPriorityMatchingRuleRegardlessOfListOrder() {
		CategorisationRule lowPriority = ruleWithPriority(10, MatchOperator.CONTAINS, "SHELL", Direction.DEBIT);
		CategorisationRule higherPriority = ruleWithPriority(20, MatchOperator.CONTAINS, "SHELL", Direction.DEBIT);

		Optional<CategorisationRule> selected = CategorisationRuleEngine.selectRule(
				List.of(higherPriority, lowPriority), "Shell Garage", "fuel", Direction.DEBIT);

		assertThat(selected).contains(lowPriority);
	}

	@Test
	void skipsNonMatchingRulesAndSelectsTheNextMatch() {
		CategorisationRule nonMatching = ruleWithPriority(10, MatchOperator.CONTAINS, "CHECKERS", Direction.DEBIT);
		CategorisationRule matching = ruleWithPriority(20, MatchOperator.CONTAINS, "SHELL", Direction.DEBIT);

		Optional<CategorisationRule> selected = CategorisationRuleEngine.selectRule(
				List.of(nonMatching, matching), "Shell Garage", "fuel", Direction.DEBIT);

		assertThat(selected).contains(matching);
	}

	@Test
	void returnsEmptyWhenNoRuleMatches() {
		CategorisationRule rule = ruleWithPriority(10, MatchOperator.CONTAINS, "CHECKERS", Direction.DEBIT);

		Optional<CategorisationRule> selected = CategorisationRuleEngine.selectRule(
				List.of(rule), "Shell Garage", "fuel", Direction.DEBIT);

		assertThat(selected).isEmpty();
	}

	@Test
	void returnsEmptyForAnEmptyRuleList() {
		assertThat(CategorisationRuleEngine.selectRule(List.of(), "Shell", "fuel", Direction.DEBIT)).isEmpty();
	}

	@Test
	void rejectsNullCandidateRules() {
		assertThatNullPointerException()
				.isThrownBy(() -> CategorisationRuleEngine.selectRule(null, "Shell", "fuel", Direction.DEBIT));
	}

	@Test
	void rejectsNullDirection() {
		assertThatNullPointerException()
				.isThrownBy(() -> CategorisationRuleEngine.selectRule(List.of(), "Shell", "fuel", null));
	}

}
