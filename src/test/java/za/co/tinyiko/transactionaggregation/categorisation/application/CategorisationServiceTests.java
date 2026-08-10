package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRule;
import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRuleId;
import za.co.tinyiko.transactionaggregation.categorisation.domain.Direction;
import za.co.tinyiko.transactionaggregation.categorisation.domain.MatchField;
import za.co.tinyiko.transactionaggregation.categorisation.domain.MatchOperator;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategory;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategoryId;
import za.co.tinyiko.transactionaggregation.categorisation.port.ActiveCategorisationRule;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategorisationRuleRepositoryPort;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategoryRepositoryPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategorisationServiceTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-31T08:15:00Z"), ZoneOffset.UTC);

	@Mock
	private CategorisationRuleRepositoryPort categorisationRuleRepositoryPort;

	@Mock
	private CategoryRepositoryPort categoryRepositoryPort;

	@Test
	void returnsTheMatchedRuleCategoryAndRuleIdWithFallbackApplyFalseForAnOrdinaryCategory() {
		TransactionCategoryId categoryId = TransactionCategoryId.generate();
		CategorisationRule rule = CategorisationRule.register(
				CategorisationRuleId.generate(), categoryId, MatchField.MERCHANT, MatchOperator.CONTAINS, "SHELL", Direction.DEBIT, 30, FIXED_CLOCK);
		when(categorisationRuleRepositoryPort.findAllActive())
				.thenReturn(List.of(new ActiveCategorisationRule(rule, false)));

		CategorisationService service = new CategorisationService(categorisationRuleRepositoryPort, categoryRepositoryPort);
		CategorisationDecision decision = service.categorise(new CategorisationInput("Shell Garage", "fuel purchase", "DEBIT"));

		assertThat(decision.categoryId()).isEqualTo(categoryId.value());
		assertThat(decision.matchedRuleId()).isEqualTo(rule.id().value());
		assertThat(decision.reason()).contains("MERCHANT", "CONTAINS", "SHELL");
		assertThat(decision.fallbackApplied()).isFalse();
		verify(categoryRepositoryPort, never()).findFallback();
	}

	/**
	 * The exact scenario the whole {@code fallbackApplied} correction exists for: the seeded
	 * priority-1000 {@code REGEX '.*'} DEBIT catch-all rule is a real, matched rule (so
	 * {@code matchedRuleId != null}), yet its target category is the fallback category. Proves
	 * {@code fallbackApplied} - not {@code matchedRuleId == null} - is the correct signal, and
	 * that no {@code findFallback()} lookup happens on this, the common, rule-matched path.
	 */
	@Test
	void matchedRuleTargetingTheFallbackCategoryReportsFallbackAppliedTrueWithoutAFallbackLookup() {
		TransactionCategoryId uncategorisedId = TransactionCategoryId.generate();
		CategorisationRule catchAllRule = CategorisationRule.register(
				CategorisationRuleId.generate(), uncategorisedId, MatchField.DESCRIPTION, MatchOperator.REGEX, ".*", Direction.DEBIT, 1000, FIXED_CLOCK);
		when(categorisationRuleRepositoryPort.findAllActive())
				.thenReturn(List.of(new ActiveCategorisationRule(catchAllRule, true)));

		CategorisationService service = new CategorisationService(categorisationRuleRepositoryPort, categoryRepositoryPort);
		CategorisationDecision decision = service.categorise(new CategorisationInput(null, "unrecognised transaction", "DEBIT"));

		assertThat(decision.categoryId()).isEqualTo(uncategorisedId.value());
		assertThat(decision.matchedRuleId()).isEqualTo(catchAllRule.id().value());
		assertThat(decision.fallbackApplied()).isTrue();
		verify(categoryRepositoryPort, never()).findFallback();
	}

	@Test
	void fallsBackToTheFallbackCategoryWhenNoRuleMatchesAtAll() {
		when(categorisationRuleRepositoryPort.findAllActive()).thenReturn(List.of());
		TransactionCategoryId fallbackId = TransactionCategoryId.generate();
		TransactionCategory fallback = TransactionCategory.register(fallbackId, "UNCATEGORISED", "Uncategorised", null, true, FIXED_CLOCK);
		when(categoryRepositoryPort.findFallback()).thenReturn(Optional.of(fallback));

		CategorisationService service = new CategorisationService(categorisationRuleRepositoryPort, categoryRepositoryPort);
		CategorisationDecision decision = service.categorise(new CategorisationInput(null, "unrecognised transaction", "DEBIT"));

		assertThat(decision.categoryId()).isEqualTo(fallbackId.value());
		assertThat(decision.matchedRuleId()).isNull();
		assertThat(decision.reason()).isEqualTo("No rule matched; assigned fallback category");
		assertThat(decision.fallbackApplied()).isTrue();
	}

	@Test
	void throwsWhenNoRuleMatchesAndNoFallbackIsConfigured() {
		when(categorisationRuleRepositoryPort.findAllActive()).thenReturn(List.of());
		when(categoryRepositoryPort.findFallback()).thenReturn(Optional.empty());

		CategorisationService service = new CategorisationService(categorisationRuleRepositoryPort, categoryRepositoryPort);

		assertThatIllegalStateException()
				.isThrownBy(() -> service.categorise(new CategorisationInput(null, "unrecognised transaction", "DEBIT")));
	}

}
