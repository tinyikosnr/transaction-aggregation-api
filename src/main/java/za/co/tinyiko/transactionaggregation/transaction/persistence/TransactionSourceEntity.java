package za.co.tinyiko.transactionaggregation.transaction.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity for the {@code transaction_sources} table (V8 migration, seeded by V10).
 * Distinct from the domain {@link za.co.tinyiko.transactionaggregation.transaction.domain.TransactionSource}.
 *
 * <p>No {@code applyTo}-style mapper method exists for this entity: nothing in this branch
 * writes a source through application code, only the Flyway seed migration populates this
 * table - same reasoning as {@code categorisation.persistence.TransactionCategoryEntity}.
 * Setters still exist, purely to let {@code TransactionSourceMapperTests} construct fixture
 * instances without needing a database. No {@code @Version}: SAD 27.3/29.3 both omit a version
 * column for this table.
 */
@Entity
@Table(name = "transaction_sources")
public class TransactionSourceEntity {

	@Id
	private UUID id;

	@Column(name = "code", nullable = false, length = 50)
	private String code;

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Column(name = "status", nullable = false, length = 20)
	private String status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public TransactionSourceEntity() {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
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

}
