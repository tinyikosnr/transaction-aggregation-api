package za.co.tinyiko.transactionaggregation.api.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * The create-categorisation-rule request body (feature/category-admin, TDS 55's named
 * {@code CreateCategorisationRuleRequest}). {@code matchField}/{@code operator}/{@code direction}
 * are left as unconstrained (beyond non-blank) {@code String}s here, the same deliberate choice
 * {@code CreateTransactionRequest.direction} already makes - validating they are exactly one of
 * the real enum values is left to {@code categorisation.application.CategorisationRuleAdminService},
 * not duplicated a layer higher.
 */
public record CreateCategorisationRuleRequest(
		@NotNull UUID categoryId,
		@NotBlank String matchField,
		@NotBlank String operator,
		@NotBlank String matchValue,
		@NotBlank String direction,
		@Positive int priority
) {
}
