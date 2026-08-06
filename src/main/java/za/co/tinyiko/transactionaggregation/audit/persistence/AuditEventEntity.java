package za.co.tinyiko.transactionaggregation.audit.persistence;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity for the {@code audit_events} table (V7 migration). Distinct from the domain
 * {@link za.co.tinyiko.transactionaggregation.audit.domain.AuditEvent}.
 *
 * <p>{@code eventData} is mapped with Hibernate's native {@code @JdbcTypeCode(SqlTypes.JSON)}
 * against a plain {@code String} field - Hibernate 7 (this project's resolved version) supports
 * this directly against PostgreSQL {@code JSONB} columns with no extra dependency. Verified via
 * {@code JpaAuditRepositoryAdapterTests} against a real Testcontainers-backed PostgreSQL instance
 * rather than assumed.
 *
 * <p>No setters and no {@code @Version}: unlike the mutable-aggregate entities elsewhere in this
 * codebase, {@code audit_events} rows are append-only (SAD 27.7) and are never looked up and
 * mutated in place, so there is no {@code applyTo}-style write path needing them - only
 * {@code AuditMapper#toEntity}, which builds a fully-formed instance via the all-args
 * constructor.
 */
@Entity
@Table(name = "audit_events")
public class AuditEventEntity {

	@Id
	private UUID id;

	@Column(name = "aggregate_type", nullable = false, length = 100)
	private String aggregateType;

	@Column(name = "aggregate_id", nullable = false)
	private UUID aggregateId;

	@Column(name = "event_type", nullable = false, length = 100)
	private String eventType;

	@Column(name = "actor", nullable = false, length = 150)
	private String actor;

	@Column(name = "correlation_id", nullable = false, length = 100)
	private String correlationId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "event_data", nullable = false)
	private String eventData;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	protected AuditEventEntity() {
	}

	public AuditEventEntity(
			UUID id,
			String aggregateType,
			UUID aggregateId,
			String eventType,
			String actor,
			String correlationId,
			String eventData,
			Instant occurredAt
	) {
		this.id = id;
		this.aggregateType = aggregateType;
		this.aggregateId = aggregateId;
		this.eventType = eventType;
		this.actor = actor;
		this.correlationId = correlationId;
		this.eventData = eventData;
		this.occurredAt = occurredAt;
	}

	public UUID getId() {
		return id;
	}

	public String getAggregateType() {
		return aggregateType;
	}

	public UUID getAggregateId() {
		return aggregateId;
	}

	public String getEventType() {
		return eventType;
	}

	public String getActor() {
		return actor;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public String getEventData() {
		return eventData;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

}
