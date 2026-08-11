package za.co.tinyiko.transactionaggregation.audit.application;

/**
 * Inbound port for audit-event search (feature/audit-query, SAD 31.2's "operational and
 * compliance queries" - the only documented basis for this capability; the exact filters,
 * pagination, and sorting below are explicit project decisions, not a fully-specified SAD/TDS
 * contract). The only audit-read capability this branch implements: no get-by-id, matching the
 * "queries" (plural, search-shaped) wording rather than inventing a singular resource lookup
 * nothing names.
 */
public interface SearchAuditEventsUseCase {

	AuditEventSearchResult search(AuditEventSearchCriteria criteria);

}
