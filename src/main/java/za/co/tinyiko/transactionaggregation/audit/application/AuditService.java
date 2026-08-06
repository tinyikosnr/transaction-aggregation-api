package za.co.tinyiko.transactionaggregation.audit.application;

import java.time.Clock;

import org.springframework.stereotype.Service;

import za.co.tinyiko.transactionaggregation.audit.domain.AuditEvent;
import za.co.tinyiko.transactionaggregation.audit.domain.AuditEventId;
import za.co.tinyiko.transactionaggregation.audit.port.AuditRepositoryPort;

@Service
class AuditService implements RecordAuditEventUseCase {

	private final AuditRepositoryPort auditRepositoryPort;
	private final Clock clock;

	AuditService(AuditRepositoryPort auditRepositoryPort, Clock clock) {
		this.auditRepositoryPort = auditRepositoryPort;
		this.clock = clock;
	}

	@Override
	public AuditEvent record(RecordAuditEventCommand command) {
		AuditEvent auditEvent = AuditEvent.register(
				AuditEventId.generate(),
				command.aggregateType(),
				command.aggregateId(),
				command.eventType(),
				command.actor(),
				command.correlationId(),
				command.eventData(),
				clock);

		return auditRepositoryPort.save(auditEvent);
	}

}
