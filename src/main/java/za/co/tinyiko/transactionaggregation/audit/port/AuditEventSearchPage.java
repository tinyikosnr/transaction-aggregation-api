package za.co.tinyiko.transactionaggregation.audit.port;

import java.util.List;

/**
 * {@code audit.port}'s own result shape for {@link AuditQueryRepositoryPort#search} - just the
 * rows and the total match count; {@code audit.application} already knows the requested
 * page/size (it supplied them via {@link AuditEventSearchQuery}) and computes {@code totalPages}
 * itself, the same division of responsibility {@code transaction.port.TransactionSearchPage}
 * already establishes.
 */
public record AuditEventSearchPage(List<AuditEventRow> rows, long totalElements) {
}
