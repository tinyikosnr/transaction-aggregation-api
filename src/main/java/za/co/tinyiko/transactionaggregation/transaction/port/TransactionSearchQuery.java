package za.co.tinyiko.transactionaggregation.transaction.port;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code transaction.port}'s own internal criteria shape for
 * {@link TransactionSearchRepositoryPort#search} - primitives only, deliberately not
 * {@code transaction.application.TransactionSearchCriteria} (same "port never references
 * application" rule as the row types). Every filter field is nullable/optional except
 * {@code page}/{@code size}/{@code sortAscending}. There is no sort-field parameter: this branch
 * whitelists exactly one sortable field ({@code transactionTimestamp} -> {@code occurredAt}), so
 * only direction is a genuine variable.
 */
public record TransactionSearchQuery(
		UUID customerId,
		UUID transactionSourceId,
		UUID categoryId,
		UUID merchantId,
		String direction,
		String status,
		Instant occurredFrom,
		Instant occurredTo,
		int page,
		int size,
		boolean sortAscending
) {
}
