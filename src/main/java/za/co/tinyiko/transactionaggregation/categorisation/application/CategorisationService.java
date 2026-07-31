package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRule;
import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRuleEngine;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategory;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategoryRepositoryPort;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategorisationRuleRepositoryPort;

@Service
class CategorisationService implements CategoriseTransactionUseCase {

	private final CategorisationRuleRepositoryPort categorisationRuleRepositoryPort;
	private final CategoryRepositoryPort categoryRepositoryPort;

	CategorisationService(
			CategorisationRuleRepositoryPort categorisationRuleRepositoryPort,
			CategoryRepositoryPort categoryRepositoryPort
	) {
		this.categorisationRuleRepositoryPort = categorisationRuleRepositoryPort;
		this.categoryRepositoryPort = categoryRepositoryPort;
	}

	@Override
	public CategorisationDecision categorise(CategorisationInput input) {
		List<CategorisationRule> activeRules = categorisationRuleRepositoryPort.findAllActive();

		Optional<CategorisationRule> matched = CategorisationRuleEngine.selectRule(
				activeRules, input.merchantText(), input.description(), input.direction());

		if (matched.isPresent()) {
			CategorisationRule rule = matched.get();
			return new CategorisationDecision(rule.categoryId(), rule.id(), reasonFor(rule));
		}

		// Reached only if the seeded direction-wide catch-all rules (TDS 37, priority 999/1000)
		// are somehow missing or inactive - under normal seeded data this branch is never taken.
		TransactionCategory fallback = categoryRepositoryPort.findFallback()
				.orElseThrow(() -> new IllegalStateException("No fallback category is configured"));
		return new CategorisationDecision(fallback.id(), null, "No rule matched; assigned fallback category");
	}

	private static String reasonFor(CategorisationRule rule) {
		return "%s %s %s".formatted(rule.matchField(), rule.operator(), rule.matchValue());
	}

}
