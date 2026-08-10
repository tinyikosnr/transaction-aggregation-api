package za.co.tinyiko.transactionaggregation.audit.port;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code audit.port}'s own internal criteria shape for {@link AuditQueryRepositoryPort#search} -
 * primitives only, deliberately not {@code audit.application.AuditEventSearchCriteria} (same
 * "port never references application" rule as {@code transaction.port.TransactionSearchQuery}).
 * Every filter field is nullable/optional except {@code page}/{@code size}/{@code sortAscending}.
 * No sort-field parameter: this branch whitelists exactly one sortable field ({@code occurredAt}),
 * so only direction is a genuine variable. Blank-string normalisation and the
 * "{@code aggregateId} requires {@code aggregateType}" rule are both already resolved by the time
 * a query reaches this port - {@code audit.application} owns that validation, not the persistence
 * layer.
 */
public record AuditEventSearchQuery(
		String aggregateType,
		UUID aggregateId,
		String eventType,
		String actor,
		String correlationId,
		Instant occurredFrom,
		Instant occurredTo,
		int page,
		int size,
		boolean sortAscending
) {
}
