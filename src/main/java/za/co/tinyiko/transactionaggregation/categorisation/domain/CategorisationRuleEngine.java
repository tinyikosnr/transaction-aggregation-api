package za.co.tinyiko.transactionaggregation.categorisation.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Selects the first active rule that matches, in ascending priority order (BR-09). Merchant
 * rules being evaluated before description rules (BR-10) is not a separate phase here - it
 * falls out naturally from the seeded priority values (merchant rules are seeded at lower
 * priority numbers than description rules).
 *
 * <p>Sorts defensively rather than trusting the caller's ordering, since correctness here
 * depends entirely on iteration order.
 */
public final class CategorisationRuleEngine {

	private CategorisationRuleEngine() {
	}

	public static Optional<CategorisationRule> selectRule(
			List<CategorisationRule> candidateRules,
			String merchantText,
			String description,
			Direction direction
	) {
		Objects.requireNonNull(candidateRules, "candidateRules must not be null");
		Objects.requireNonNull(direction, "direction must not be null");

		return candidateRules.stream()
				.sorted(Comparator.comparingInt(CategorisationRule::priority))
				.filter(rule -> rule.matches(merchantText, description, direction))
				.findFirst();
	}

}
