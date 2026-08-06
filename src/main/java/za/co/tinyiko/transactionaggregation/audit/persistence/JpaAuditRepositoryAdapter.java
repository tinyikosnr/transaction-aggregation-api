package za.co.tinyiko.transactionaggregation.audit.persistence;

import org.springframework.stereotype.Component;

import za.co.tinyiko.transactionaggregation.audit.domain.AuditEvent;
import za.co.tinyiko.transactionaggregation.audit.mapper.AuditMapper;
import za.co.tinyiko.transactionaggregation.audit.port.AuditRepositoryPort;

@Component
class JpaAuditRepositoryAdapter implements AuditRepositoryPort {

	private final SpringDataAuditRepository springDataAuditRepository;

	JpaAuditRepositoryAdapter(SpringDataAuditRepository springDataAuditRepository) {
		this.springDataAuditRepository = springDataAuditRepository;
	}

	@Override
	public AuditEvent save(AuditEvent auditEvent) {
		AuditEventEntity entity = AuditMapper.toEntity(auditEvent);
		AuditEventEntity saved = springDataAuditRepository.save(entity);
		return AuditMapper.toDomain(saved);
	}

}
