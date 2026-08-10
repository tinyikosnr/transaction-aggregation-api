package za.co.tinyiko.transactionaggregation.audit.port;

/**
 * Outbound port the audit application layer uses to search audit events (feature/audit-query) -
 * a separate port from {@link za.co.tinyiko.transactionaggregation.audit.port.AuditRepositoryPort},
 * the same "write port stays single-purpose, search gets its own port" split
 * {@code transaction}'s {@code TransactionRepositoryPort}/{@code TransactionSearchRepositoryPort}
 * already established. Exactly one method, deliberately: this port can never be used to write,
 * update, or delete an audit event - the append-only guarantee holds structurally, not just by
 * convention, since nothing here could mutate {@code audit_events} even if a caller tried.
 */
public interface AuditQueryRepositoryPort {

	AuditEventSearchPage search(AuditEventSearchQuery query);

}
