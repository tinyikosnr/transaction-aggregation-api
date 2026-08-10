package za.co.tinyiko.transactionaggregation.api.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The create-categorisation-rule request body (feature/category-admin, TDS 55's named
 * {@code CreateCategorisationRuleRequest}). {@code matchField}/{@code operator}/{@code direction}
 * are left as unconstrained (beyond non-blank) {@code String}s here, the same deliberate choice
 * {@code CreateTransactionRequest.direction} already makes - validating they are exactly one of
 * the real enum values is left to {@code categorisation.application.CategorisationRuleAdminService},
 * not duplicated a layer higher.
 */
public record CreateCategorisationRuleRequest(
		@Schema(description = "Category assigned when this rule matches. Must reference an existing category.", requiredMode = Schema.RequiredMode.REQUIRED)
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
		@Positive int priority
) {
}
