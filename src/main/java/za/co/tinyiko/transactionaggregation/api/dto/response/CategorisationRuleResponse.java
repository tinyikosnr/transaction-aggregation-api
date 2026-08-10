package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The categorisation-rule admin response (feature/category-admin) - list/get/create/update all
 * return this same shape. {@code version} lets a client obtain the current optimistic-locking
 * value from any read and echo it back as {@code expectedVersion} on its next {@code PUT}.
 */
@Schema(description = "A categorisation rule. priority is not required to be unique or dense across rules - duplicate priorities are permitted and evaluated in an unspecified order among themselves.")
public record CategorisationRuleResponse(
		@Schema(description = "Rule identifier.") UUID id,
		@Schema(description = "Category assigned when this rule matches. Mutable on update.") UUID categoryId,
		@Schema(description = "Transaction field this rule matches against.", allowableValues = {"MERCHANT", "DESCRIPTION"}) String matchField,
		@Schema(description = "How matchValue is compared against matchField.", allowableValues = {"EQUALS", "CONTAINS", "REGEX"}) String operator,
		@Schema(description = "Value/pattern to match.", example = "SHOPRITE") String matchValue,
		@Schema(description = "Transaction direction this rule applies to.", allowableValues = {"CREDIT", "DEBIT"}) String direction,
		@Schema(description = "Evaluation priority; lower values are evaluated first. Not required to be unique or dense.") int priority,
		@Schema(description = "Whether this rule is currently evaluated by the categorisation engine.") boolean active,
		@Schema(description = "Optimistic-locking version. Echo the value from your last read as expectedVersion on PUT.") long version,
		@Schema(description = "Timestamp the rule was created, in UTC.") Instant createdAt,
		@Schema(description = "Timestamp the rule was last updated, in UTC.") Instant updatedAt
) {
}
