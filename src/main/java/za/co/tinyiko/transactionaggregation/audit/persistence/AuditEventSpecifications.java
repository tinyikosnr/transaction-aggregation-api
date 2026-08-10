package za.co.tinyiko.transactionaggregation.audit.persistence;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

/**
 * One small, named {@link Specification} per optional search filter (feature/audit-query),
 * composed by {@link JpaAuditQueryRepositoryAdapter} via {@code Specification.allOf(...)} - the
 * same pattern {@code transaction.persistence.TransactionSpecifications} already establishes.
 * Every filter here is genuinely optional and independently combinable. Returning {@code null}
 * for an absent filter is the documented Spring Data idiom: a {@code null} {@link Specification}
 * composed via {@code Specification.allOf} contributes no restriction.
 *
 * <p>By the time a query reaches here, {@code audit.application} has already normalised blank
 * strings to {@code null} and enforced "{@code aggregateId} requires {@code aggregateType}" - this
 * class only ever sees already-valid, already-normalised input.
 */
final class AuditEventSpecifications {

	private AuditEventSpecifications() {
	}

	static Specification<AuditEventEntity> hasAggregateType(String aggregateType) {
		return aggregateType == null ? null
				: (root, query, cb) -> cb.equal(root.get("aggregateType"), aggregateType);
	}

	static Specification<AuditEventEntity> hasAggregateId(UUID aggregateId) {
		return aggregateId == null ? null
				: (root, query, cb) -> cb.equal(root.get("aggregateId"), aggregateId);
	}

	static Specification<AuditEventEntity> hasEventType(String eventType) {
		return eventType == null ? null
				: (root, query, cb) -> cb.equal(root.get("eventType"), eventType);
	}

	static Specification<AuditEventEntity> hasActor(String actor) {
		return actor == null ? null
				: (root, query, cb) -> cb.equal(root.get("actor"), actor);
	}

	static Specification<AuditEventEntity> hasCorrelationId(String correlationId) {
		return correlationId == null ? null
				: (root, query, cb) -> cb.equal(root.get("correlationId"), correlationId);
	}

	/**
	 * Both bounds inclusive; either may be absent independently, leaving that side of the range
	 * open. No maximum span is enforced here or anywhere else - an explicit project decision
	 * (feature/audit-query), not an oversight.
	 */
	static Specification<AuditEventEntity> occurredBetween(Instant occurredFrom, Instant occurredTo) {
		if (occurredFrom != null && occurredTo != null) {
			return (root, query, cb) -> cb.between(root.get("occurredAt"), occurredFrom, occurredTo);
		}
		if (occurredFrom != null) {
			return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), occurredFrom);
		}
		if (occurredTo != null) {
			return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("occurredAt"), occurredTo);
		}
		return null;
	}

}
