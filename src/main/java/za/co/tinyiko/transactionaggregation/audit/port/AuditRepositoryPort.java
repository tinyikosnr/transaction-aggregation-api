package za.co.tinyiko.transactionaggregation.audit.port;

import za.co.tinyiko.transactionaggregation.audit.domain.AuditEvent;

/**
 * Outbound port the audit application layer uses to persist audit events (TDS 60).
 *
 * <p>Write-only, the mirror case of categorisation's read-only ports: nothing reads an
 * {@link AuditEvent} back yet (no documented query capability exists despite the
 * {@code AUDIT_READ} authority being reserved for one), so only {@code save} is defined.
 */
public interface AuditRepositoryPort {

	AuditEvent save(AuditEvent auditEvent);

}
