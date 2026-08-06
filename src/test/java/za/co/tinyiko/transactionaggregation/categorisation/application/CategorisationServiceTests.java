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
import za.co.tinyiko.transactionaggregation.categorisation.port.CategorisationRuleRepositoryPort;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategoryRepositoryPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategorisationServiceTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-31T08:15:00Z"), ZoneOffset.UTC);

	@Mock
	private CategorisationRuleRepositoryPort categorisationRuleRepositoryPort;

	@Mock
	private CategoryRepositoryPort categoryRepositoryPort;

	@Test
	void returnsTheMatchedRuleCategoryAndRuleId() {
		TransactionCategoryId categoryId = TransactionCategoryId.generate();
		CategorisationRule rule = CategorisationRule.register(
				CategorisationRuleId.generate(), categoryId, MatchField.MERCHANT, MatchOperator.CONTAINS, "SHELL", Direction.DEBIT, 30, FIXED_CLOCK);
		when(categorisationRuleRepositoryPort.findAllActive()).thenReturn(List.of(rule));

		CategorisationService service = new CategorisationService(categorisationRuleRepositoryPort, categoryRepositoryPort);
		CategorisationDecision decision = service.categorise(new CategorisationInput("Shell Garage", "fuel purchase", Direction.DEBIT));

		assertThat(decision.categoryId()).isEqualTo(categoryId);
		assertThat(decision.matchedRuleId()).isEqualTo(rule.id());
		assertThat(decision.reason()).contains("MERCHANT", "CONTAINS", "SHELL");
	}

	@Test
	void fallsBackToTheFallbackCategoryWhenNoRuleMatches() {
		when(categorisationRuleRepositoryPort.findAllActive()).thenReturn(List.of());
		TransactionCategoryId fallbackId = TransactionCategoryId.generate();
		TransactionCategory fallback = TransactionCategory.register(fallbackId, "UNCATEGORISED", "Uncategorised", null, true, FIXED_CLOCK);
		when(categoryRepositoryPort.findFallback()).thenReturn(Optional.of(fallback));

		CategorisationService service = new CategorisationService(categorisationRuleRepositoryPort, categoryRepositoryPort);
		CategorisationDecision decision = service.categorise(new CategorisationInput(null, "unrecognised transaction", Direction.DEBIT));

		assertThat(decision.categoryId()).isEqualTo(fallbackId);
		assertThat(decision.matchedRuleId()).isNull();
		assertThat(decision.reason()).isEqualTo("No rule matched; assigned fallback category");
	}

	@Test
	void throwsWhenNoRuleMatchesAndNoFallbackIsConfigured() {
		when(categorisationRuleRepositoryPort.findAllActive()).thenReturn(List.of());
		when(categoryRepositoryPort.findFallback()).thenReturn(Optional.empty());

		CategorisationService service = new CategorisationService(categorisationRuleRepositoryPort, categoryRepositoryPort);

		assertThatIllegalStateException()
				.isThrownBy(() -> service.categorise(new CategorisationInput(null, "unrecognised transaction", Direction.DEBIT)));
	}

}
