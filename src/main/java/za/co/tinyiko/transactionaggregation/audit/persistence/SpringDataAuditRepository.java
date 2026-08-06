package za.co.tinyiko.transactionaggregation.audit.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAuditRepository extends JpaRepository<AuditEventEntity, UUID> {
}
