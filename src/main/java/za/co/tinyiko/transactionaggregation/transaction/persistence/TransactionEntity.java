package za.co.tinyiko.transactionaggregation.transaction.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * JPA entity for the {@code transactions} table (V9 migration). Distinct from the domain
 * {@link za.co.tinyiko.transactionaggregation.transaction.domain.Transaction}.
 *
 * <p>No setters and no {@code applyTo}: in this branch a transaction is only ever created, never
 * looked up and mutated - same reasoning as {@code audit.persistence.AuditEventEntity}. Only
 * {@code TransactionMapper#toEntity}, via the all-args constructor, is used to build one.
 *
 * <p>Unlike {@code AuditEventEntity}, this one does carry {@code @Version}: SAD 27.2/29.3 both
 * list a version column for {@code transactions} (backed by ADR-014), even though nothing in
 * this branch's code paths currently updates a persisted row - kept for schema consistency with
 * the documented design, the same reasoning already applied to {@code merchants.version}.
 */
@Entity
@Table(name = "transactions")
public class TransactionEntity {

	@Id
	private UUID id;

	@Column(name = "customer_id", nullable = false)
	private UUID customerId;

	@Column(name = "transaction_source_id", nullable = false)
	private UUID transactionSourceId;

	@Column(name = "external_transaction_id", nullable = false, length = 150)
	private String externalTransactionId;

	@Column(name = "merchant_id")
	private UUID merchantId;

	@Column(name = "category_id", nullable = false)
	private UUID categoryId;

	@Column(name = "amount", nullable = false, precision = 19, scale = 4)
	private BigDecimal amount;

	@Column(name = "currency", nullable = false, length = 3)
	private String currency;

	@Column(name = "direction", nullable = false, length = 10)
	private String direction;

	@Column(name = "description", length = 500)
	private String description;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	@Column(name = "received_at", nullable = false)
	private Instant receivedAt;

	@Column(name = "status", nullable = false, length = 20)
	private String status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	protected TransactionEntity() {
	}

	public TransactionEntity(
			UUID id,
			UUID customerId,
			UUID transactionSourceId,
			String externalTransactionId,
			UUID merchantId,
			UUID categoryId,
			BigDecimal amount,
			String currency,
			String direction,
			String description,
			Instant occurredAt,
			Instant receivedAt,
			String status,
			Instant createdAt
	) {
		this.id = id;
		this.customerId = customerId;
		this.transactionSourceId = transactionSourceId;
		this.externalTransactionId = externalTransactionId;
		this.merchantId = merchantId;
		this.categoryId = categoryId;
		this.amount = amount;
		this.currency = currency;
		this.direction = direction;
		this.description = description;
		this.occurredAt = occurredAt;
		this.receivedAt = receivedAt;
		this.status = status;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getCustomerId() {
		return customerId;
	}

	public UUID getTransactionSourceId() {
		return transactionSourceId;
	}

	public String getExternalTransactionId() {
		return externalTransactionId;
	}

	public UUID getMerchantId() {
		return merchantId;
	}

	public UUID getCategoryId() {
		return categoryId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getCurrency() {
		return currency;
	}

	public String getDirection() {
		return direction;
	}

	public String getDescription() {
		return description;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public Instant getReceivedAt() {
		return receivedAt;
	}

	public String getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Long getVersion() {
		return version;
	}

}
