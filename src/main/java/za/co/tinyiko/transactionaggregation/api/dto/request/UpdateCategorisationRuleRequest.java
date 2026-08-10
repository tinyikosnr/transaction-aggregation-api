package za.co.tinyiko.transactionaggregation.api.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The update-categorisation-rule request body (feature/category-admin, TDS 55's named
 * {@code UpdateCategorisationRuleRequest}) - full-replacement {@code PUT} semantics (SAD 34.2),
 * every field required. {@code active}/{@code expectedVersion} are boxed ({@code Boolean}/
 * {@code Long}), not primitive, specifically so a client omitting either fails Bean Validation
 * with a clear 400 instead of silently defaulting to {@code false}/{@code 0} - a real risk for
 * both an activation flag and an optimistic-locking version on a full-replacement endpoint.
 */
@Schema(description = "Full replacement of a categorisation rule (PUT semantics - every field, including unchanged ones, must be supplied).")
public record UpdateCategorisationRuleRequest(
		@Schema(description = "Category assigned when this rule matches. Mutable - may differ from the rule's current category. Must reference an existing category.",
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull UUID categoryId,

		@Schema(description = "Transaction field this rule matches against.", allowableValues = {"MERCHANT", "DESCRIPTION"},
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank String matchField,

		@Schema(description = "How matchValue is compared against matchField.", allowableValues = {"EQUALS", "CONTAINS", "REGEX"},
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank String operator,

		@Schema(description = "Value/pattern to match.", example = "SHOPRITE", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank String matchValue,

		@Schema(description = "Transaction direction this rule applies to.", allowableValues = {"CREDIT", "DEBIT"},
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank String direction,

		@Schema(description = "Evaluation priority; lower values are evaluated first. Not required to be unique or dense across rules.",
				example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
		@Positive int priority,

		@Schema(description = "Whether this rule should be active after the update. Required - omitting it fails validation rather than silently defaulting to false.",
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull Boolean active,

		@Schema(description = "The version read from the rule's last GET, used for optimistic-locking. A stale value yields a 409 OPTIMISTIC_LOCK_CONFLICT.",
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull Long expectedVersion
) {
}
