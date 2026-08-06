package za.co.tinyiko.transactionaggregation.categorisation.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * The identity of a {@link CategorisationRule}. Same generated-outside-the-aggregate pattern
 * as every other identity value object in this codebase.
 */
public record CategorisationRuleId(UUID value) {

	public CategorisationRuleId {
		Objects.requireNonNull(value, "value must not be null");
	}

	public static CategorisationRuleId generate() {
		return new CategorisationRuleId(UUID.randomUUID());
	}

}
