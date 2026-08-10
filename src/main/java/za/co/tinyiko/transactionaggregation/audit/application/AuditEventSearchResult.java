package za.co.tinyiko.transactionaggregation.audit.application;

import java.util.List;

/**
 * The complete outcome of {@link SearchAuditEventsUseCase#search} (feature/audit-query) - a
 * dedicated, non-generic paging result owned entirely by {@code audit.application}, not
 * {@code transaction.application.PagedResult<T>} promoted to {@code shared}. Promoting an
 * already-shipped, already-tested, cross-module-exposed type purely to save a handful of
 * duplicated fields was considered and rejected: it would force real, unforced churn onto a
 * working `transaction` contract for a marginal DRY gain, and would give `audit` a reason to
 * reach outside its own module for something this simple - the smallest-coupling option is for
 * each module to own its own small paging result. See CLAUDE.md for the full reasoning.
 */
public record AuditEventSearchResult(List<AuditEventView> content, int page, int size, long totalElements, int totalPages) {
}
