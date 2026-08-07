package za.co.tinyiko.transactionaggregation.audit.application;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

	/**
	 * Always runs in its own, independent transaction: an audit record must survive even when
	 * the operation it describes fails and rolls back its own transaction (e.g. a rejected
	 * transaction's "duplicate rejected" audit event must not disappear along with the rollback
	 * of the rejected attempt). The standard justification for {@code REQUIRES_NEW} in audit
	 * logging generally, not specific to any one caller.
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void record(RecordAuditEventCommand command) {
		AuditEvent auditEvent = AuditEvent.register(
				AuditEventId.generate(),
				command.aggregateType(),
				command.aggregateId(),
				command.eventType(),
				command.actor(),
				command.correlationId(),
				command.eventData(),
				clock);

		auditRepositoryPort.save(auditEvent);
	}

}
