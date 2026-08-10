package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.time.Instant;
import java.util.UUID;

/**
 * The admin read shape of a categorisation rule (feature/category-admin). {@code version} is a
 * plain {@code long}, copied straight through from {@code categorisation.port.CategorisationRuleRow} -
 * the only place in the application layer that carries the persistence-originated optimistic-lock
 * value, deliberately never a JPA/Hibernate type. A client obtains the current {@code version}
 * from a GET response and must echo it back as {@code expectedVersion} on the next {@code PUT}.
 */
public record CategorisationRuleView(
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
