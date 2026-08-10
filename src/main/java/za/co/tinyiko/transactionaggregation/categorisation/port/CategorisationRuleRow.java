package za.co.tinyiko.transactionaggregation.categorisation.port;

import java.time.Instant;
import java.util.UUID;

/**
 * The admin persistence path's own row shape (feature/category-admin) - deliberately distinct
 * from the domain {@link za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRule},
 * the same "port owns its own shape" rule already established by {@code transaction.port}'s
 * {@code TransactionDetailRow}/{@code CustomerTotalsRow}. Exists specifically to carry
 * {@code version} - a genuine persistence concept (JPA {@code @Version}) - out to the
 * application layer as a plain {@code long}, without the domain aggregate ever needing to know
 * about optimistic locking at all. {@code categorisation.domain.CategorisationRule} stays exactly
 * as it was before this branch: no version field, no persistence-flavoured concept anywhere in it.
 */
public record CategorisationRuleRow(
		UUID id,
		UUID categoryId,
		String matchField,
		String operator,
		String matchValue,
		String direction,
		int priority,
		boolean active,
		Instant createdAt,
		Instant updatedAt,
		long version
) {
}
