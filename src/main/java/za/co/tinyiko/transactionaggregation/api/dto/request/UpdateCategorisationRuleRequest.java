package za.co.tinyiko.transactionaggregation.api.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * The update-categorisation-rule request body (feature/category-admin, TDS 55's named
 * {@code UpdateCategorisationRuleRequest}) - full-replacement {@code PUT} semantics (SAD 34.2),
 * every field required. {@code active}/{@code expectedVersion} are boxed ({@code Boolean}/
 * {@code Long}), not primitive, specifically so a client omitting either fails Bean Validation
 * with a clear 400 instead of silently defaulting to {@code false}/{@code 0} - a real risk for
 * both an activation flag and an optimistic-locking version on a full-replacement endpoint.
 */
public record UpdateCategorisationRuleRequest(
		@NotNull UUID categoryId,
		@NotBlank String matchField,
		@NotBlank String operator,
		@NotBlank String matchValue,
		@NotBlank String direction,
		@Positive int priority,
		@NotNull Boolean active,
		@NotNull Long expectedVersion
) {
}
