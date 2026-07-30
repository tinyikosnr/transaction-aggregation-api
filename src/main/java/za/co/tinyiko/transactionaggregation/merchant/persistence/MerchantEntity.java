package za.co.tinyiko.transactionaggregation.merchant.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * JPA entity for the {@code merchants} table (V2 migration). Distinct from the domain
 * {@link za.co.tinyiko.transactionaggregation.merchant.domain.Merchant} — carries framework
 * annotations and the optimistic-locking version; the domain aggregate carries neither.
 *
 * <p>Public, with public accessors, so {@code merchant.mapper.MerchantMapper} can read and
 * populate it.
 */
@Entity
@Table(name = "merchants")
public class MerchantEntity {

	@Id
	private UUID id;

	@Column(name = "normalised_name", nullable = false, length = 255)
	private String normalisedName;

	@Column(name = "display_name", nullable = false, length = 255)
	private String displayName;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	public MerchantEntity() {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getNormalisedName() {
		return normalisedName;
	}

	public void setNormalisedName(String normalisedName) {
		this.normalisedName = normalisedName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
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
