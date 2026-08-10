package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRule;
import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRuleEngine;
import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRuleId;
import za.co.tinyiko.transactionaggregation.categorisation.domain.Direction;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategory;
import za.co.tinyiko.transactionaggregation.categorisation.port.ActiveCategorisationRule;
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
		List<ActiveCategorisationRule> activeRules = categorisationRuleRepositoryPort.findAllActive();
		Direction direction = Direction.valueOf(input.direction());

		List<CategorisationRule> rules = activeRules.stream().map(ActiveCategorisationRule::rule).toList();
		Optional<CategorisationRule> matched = CategorisationRuleEngine.selectRule(
				rules, input.merchantText(), input.description(), direction);

		if (matched.isPresent()) {
			CategorisationRule rule = matched.get();
			boolean fallbackApplied = fallbackByRuleId(activeRules).getOrDefault(rule.id(), false);
			return new CategorisationDecision(rule.categoryId().value(), rule.id().value(), reasonFor(rule), fallbackApplied);
		}

		// Reached only if the seeded direction-wide catch-all rules (TDS 37, priority 999/1000)
		// are somehow missing or inactive - under normal seeded data this branch is never taken.
		// This is the one place a dedicated fallback lookup is genuinely required - not on every
		// categorisation call (see ActiveCategorisationRule's own Javadoc).
		TransactionCategory fallback = categoryRepositoryPort.findFallback()
				.orElseThrow(() -> new IllegalStateException("No fallback category is configured"));
		return new CategorisationDecision(fallback.id().value(), null, "No rule matched; assigned fallback category", true);
	}

	private static Map<CategorisationRuleId, Boolean> fallbackByRuleId(List<ActiveCategorisationRule> activeRules) {
		Map<CategorisationRuleId, Boolean> fallbackByRuleId = new HashMap<>();
		for (ActiveCategorisationRule activeRule : activeRules) {
			fallbackByRuleId.put(activeRule.rule().id(), activeRule.categoryIsFallback());
		}
		return fallbackByRuleId;
	}

	private static String reasonFor(CategorisationRule rule) {
		return "%s %s %s".formatted(rule.matchField(), rule.operator(), rule.matchValue());
	}

}
