package za.co.tinyiko.transactionaggregation.transaction.mapper;

import za.co.tinyiko.transactionaggregation.transaction.domain.Money;
import za.co.tinyiko.transactionaggregation.transaction.domain.Transaction;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionDirection;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionId;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionSourceId;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionStatus;
import za.co.tinyiko.transactionaggregation.transaction.persistence.TransactionEntity;

/**
 * Maps between the domain {@link Transaction} and the JPA {@link TransactionEntity}.
 *
 * <p>{@code toEntity}, not {@code applyTo}: a transaction is never looked up and mutated in
 * place in this branch (immutable once processed, SAD 27.2), so the mapper builds a fresh,
 * fully-formed {@link TransactionEntity} via its all-args constructor rather than mutating one
 * passed in - same reasoning as {@code audit.mapper.AuditMapper}.
 */
public final class TransactionMapper {

	private TransactionMapper() {
	}

	public static Transaction toDomain(TransactionEntity entity) {
		return Transaction.reconstitute(
				new TransactionId(entity.getId()),
				entity.getCustomerId(),
				new TransactionSourceId(entity.getTransactionSourceId()),
				entity.getExternalTransactionId(),
				entity.getMerchantId(),
				entity.getCategoryId(),
				new Money(entity.getAmount(), entity.getCurrency()),
				TransactionDirection.valueOf(entity.getDirection()),
				entity.getDescription(),
				entity.getOccurredAt(),
				entity.getReceivedAt(),
				TransactionStatus.valueOf(entity.getStatus()),
				entity.getCreatedAt());
	}

	public static TransactionEntity toEntity(Transaction transaction) {
		return new TransactionEntity(
				transaction.id().value(),
				transaction.customerId(),
				transaction.transactionSourceId().value(),
				transaction.externalTransactionId(),
				transaction.merchantId(),
				transaction.categoryId(),
				transaction.amount().amount(),
				transaction.amount().currency(),
				transaction.direction().name(),
				transaction.description(),
				transaction.occurredAt(),
				transaction.receivedAt(),
				transaction.status().name(),
				transaction.createdAt());
	}

}
