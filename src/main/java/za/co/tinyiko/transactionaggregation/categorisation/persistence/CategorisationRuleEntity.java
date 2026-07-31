package za.co.tinyiko.transactionaggregation.categorisation.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import za.co.tinyiko.transactionaggregation.categorisation.domain.Direction;
import za.co.tinyiko.transactionaggregation.categorisation.domain.MatchField;
import za.co.tinyiko.transactionaggregation.categorisation.domain.MatchOperator;

/**
 * JPA entity for the {@code categorisation_rules} table (V4 migration, seeded by V6).
 * {@code categoryId} is a scalar UUID, not a JPA relationship, even though both tables belong
 * to this same module - matching this codebase's consistent preference for simple,
 * mapper-driven persistence over ORM relationships, rather than introducing the first one here.
 *
 * <p>No {@code applyTo}-style mapper method exists for this entity either - same reasoning as
 * {@link TransactionCategoryEntity}. Setters exist only to support test fixture construction.
 */
@Entity
@Table(name = "categorisation_rules")
public class CategorisationRuleEntity {

	@Id
	private UUID id;

	@Column(name = "category_id", nullable = false)
	private UUID categoryId;

	@Enumerated(EnumType.STRING)
	@Column(name = "match_field", nullable = false, length = 20)
	private MatchField matchField;

	@Enumerated(EnumType.STRING)
	@Column(name = "operator", nullable = false, length = 20)
	private MatchOperator operator;

	@Column(name = "match_value", nullable = false, length = 200)
	private String matchValue;

	@Enumerated(EnumType.STRING)
	@Column(name = "direction", nullable = false, length = 10)
	private Direction direction;

	@Column(name = "priority", nullable = false)
	private int priority;

	@Column(name = "active", nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	public CategorisationRuleEntity() {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(UUID categoryId) {
		this.categoryId = categoryId;
	}

	public MatchField getMatchField() {
		return matchField;
	}

	public void setMatchField(MatchField matchField) {
		this.matchField = matchField;
	}

	public MatchOperator getOperator() {
		return operator;
	}

	public void setOperator(MatchOperator operator) {
		this.operator = operator;
	}

	public String getMatchValue() {
		return matchValue;
	}

	public void setMatchValue(String matchValue) {
		this.matchValue = matchValue;
	}

	public Direction getDirection() {
		return direction;
	}

	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	public Long getVersion() {
		return version;
	}

}
