package za.co.tinyiko.transactionaggregation.transaction.mapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.transaction.domain.Money;
import za.co.tinyiko.transactionaggregation.transaction.domain.Transaction;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionDirection;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionId;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionSourceId;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionStatus;
import za.co.tinyiko.transactionaggregation.transaction.persistence.TransactionEntity;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionMapperTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-06T08:15:00Z"), ZoneOffset.UTC);

	@Test
	void toDomainMapsAllFields() {
		UUID id = UUID.randomUUID();
		UUID customerId = UUID.randomUUID();
		UUID sourceId = UUID.randomUUID();
		UUID merchantId = UUID.randomUUID();
		UUID categoryId = UUID.randomUUID();
		TransactionEntity entity = new TransactionEntity(
				id, customerId, sourceId, "EXT-001", merchantId, categoryId,
				new BigDecimal("125.5000"), "ZAR", "DEBIT", "Checkers Centurion",
				Instant.parse("2026-08-06T08:00:00Z"), Instant.parse("2026-08-06T08:15:00Z"),
				"PROCESSED", Instant.parse("2026-08-06T08:15:00Z"));

		Transaction transaction = TransactionMapper.toDomain(entity);

		assertThat(transaction.id()).isEqualTo(new TransactionId(id));
		assertThat(transaction.customerId()).isEqualTo(customerId);
		assertThat(transaction.transactionSourceId()).isEqualTo(new TransactionSourceId(sourceId));
		assertThat(transaction.externalTransactionId()).isEqualTo("EXT-001");
		assertThat(transaction.merchantId()).isEqualTo(merchantId);
		assertThat(transaction.categoryId()).isEqualTo(categoryId);
		assertThat(transaction.amount()).isEqualTo(new Money(new BigDecimal("125.50"), "ZAR"));
		assertThat(transaction.direction()).isEqualTo(TransactionDirection.DEBIT);
		assertThat(transaction.description()).isEqualTo("Checkers Centurion");
		assertThat(transaction.occurredAt()).isEqualTo(Instant.parse("2026-08-06T08:00:00Z"));
		assertThat(transaction.receivedAt()).isEqualTo(Instant.parse("2026-08-06T08:15:00Z"));
		assertThat(transaction.status()).isEqualTo(TransactionStatus.PROCESSED);
		assertThat(transaction.createdAt()).isEqualTo(Instant.parse("2026-08-06T08:15:00Z"));
	}

	@Test
	void toEntityCopiesAllFields() {
		UUID customerId = UUID.randomUUID();
		UUID merchantId = UUID.randomUUID();
		UUID categoryId = UUID.randomUUID();
		Transaction transaction = Transaction.register(
				TransactionId.generate(), customerId, TransactionSourceId.generate(), "EXT-001", merchantId,
				categoryId, new Money(new BigDecimal("125.50"), "ZAR"), TransactionDirection.DEBIT,
				"Checkers Centurion", Instant.parse("2026-08-06T08:00:00Z"), FIXED_CLOCK);

		TransactionEntity entity = TransactionMapper.toEntity(transaction);

		assertThat(entity.getId()).isEqualTo(transaction.id().value());
		assertThat(entity.getCustomerId()).isEqualTo(customerId);
		assertThat(entity.getTransactionSourceId()).isEqualTo(transaction.transactionSourceId().value());
		assertThat(entity.getExternalTransactionId()).isEqualTo("EXT-001");
		assertThat(entity.getMerchantId()).isEqualTo(merchantId);
		assertThat(entity.getCategoryId()).isEqualTo(categoryId);
		assertThat(entity.getAmount()).isEqualByComparingTo("125.50");
		assertThat(entity.getCurrency()).isEqualTo("ZAR");
		assertThat(entity.getDirection()).isEqualTo("DEBIT");
		assertThat(entity.getDescription()).isEqualTo("Checkers Centurion");
		assertThat(entity.getOccurredAt()).isEqualTo(Instant.parse("2026-08-06T08:00:00Z"));
		assertThat(entity.getReceivedAt()).isEqualTo(transaction.receivedAt());
		assertThat(entity.getStatus()).isEqualTo("PROCESSED");
		assertThat(entity.getCreatedAt()).isEqualTo(transaction.createdAt());
	}

	@Test
	void toEntityHandlesNullMerchantIdAndDescription() {
		Transaction transaction = Transaction.register(
				TransactionId.generate(), UUID.randomUUID(), TransactionSourceId.generate(), "EXT-001", null,
				UUID.randomUUID(), new Money(new BigDecimal("50.00"), "ZAR"), TransactionDirection.CREDIT,
				null, Instant.parse("2026-08-06T08:00:00Z"), FIXED_CLOCK);

		TransactionEntity entity = TransactionMapper.toEntity(transaction);

		assertThat(entity.getMerchantId()).isNull();
		assertThat(entity.getDescription()).isNull();
	}

}
