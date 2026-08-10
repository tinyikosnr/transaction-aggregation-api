package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * The categorisation-rule admin response (feature/category-admin) - list/get/create/update all
 * return this same shape. {@code version} lets a client obtain the current optimistic-locking
 * value from any read and echo it back as {@code expectedVersion} on its next {@code PUT}.
 */
public record CategorisationRuleResponse(
		UUID id,
		UUID categoryId,
		String matchField,
		String operator,
		String matchValue,
		String direction,
		int priority,
		boolean active,
		long version,
		Instant createdAt,
		Instant updatedAt
) {
}
