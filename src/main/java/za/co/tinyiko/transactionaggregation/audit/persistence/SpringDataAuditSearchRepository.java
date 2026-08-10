package za.co.tinyiko.transactionaggregation.audit.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;

/**
 * The read-side Spring Data interface for {@code audit_events} (feature/audit-query) -
 * deliberately {@code Repository<AuditEventEntity, UUID>} (Spring Data's bare marker interface,
 * no methods of its own), not {@code JpaRepository}: {@code JpaSpecificationExecutor} declares
 * {@code findAll(Specification, Pageable)}/{@code findAll(Specification)}/{@code count(Specification)}
 * itself, so this combination is sufficient for Specification-based paginated search and exposes
 * no {@code save}/{@code delete}/{@code findById}/unpaged-{@code findAll} - the append-only
 * guarantee holds structurally here, not by convention alone. Kept entirely separate from the
 * existing write-side {@link SpringDataAuditRepository} (which stays {@code JpaRepository}-based,
 * unchanged), the same read/write repository split {@code transaction.persistence} already
 * establishes between {@code SpringDataTransactionRepository} and
 * {@code SpringDataTransactionSearchRepository}.
 */
interface SpringDataAuditSearchRepository extends Repository<AuditEventEntity, UUID>, JpaSpecificationExecutor<AuditEventEntity> {
}
