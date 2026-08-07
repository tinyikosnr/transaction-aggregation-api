package za.co.tinyiko.transactionaggregation.transaction.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class TransactionTests {

	private static final Instant FIXED_INSTANT = Instant.parse("2026-08-06T08:15:00Z");
	private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
	private static final Money AN_AMOUNT = new Money(new BigDecimal("125.50"), "ZAR");

	private static TransactionId anId() {
		return TransactionId.generate();
	}

	private static TransactionSourceId aSourceId() {
		return TransactionSourceId.generate();
	}

	@Test
	void registerCreatesTransactionWithTimestampsAndProcessedStatus() {
		TransactionId id = anId();
		UUID customerId = UUID.randomUUID();
		TransactionSourceId sourceId = aSourceId();
		UUID merchantId = UUID.randomUUID();
		UUID categoryId = UUID.randomUUID();
		Instant occurredAt = FIXED_INSTANT.minusSeconds(60);

		Transaction transaction = Transaction.register(id, customerId, sourceId, "EXT-001", merchantId, categoryId,
				AN_AMOUNT, TransactionDirection.DEBIT, "Checkers Centurion", occurredAt, FIXED_CLOCK);

		assertThat(transaction.id()).isEqualTo(id);
		assertThat(transaction.customerId()).isEqualTo(customerId);
		assertThat(transaction.transactionSourceId()).isEqualTo(sourceId);
		assertThat(transaction.externalTransactionId()).isEqualTo("EXT-001");
		assertThat(transaction.merchantId()).isEqualTo(merchantId);
		assertThat(transaction.categoryId()).isEqualTo(categoryId);
		assertThat(transaction.amount()).isEqualTo(AN_AMOUNT);
		assertThat(transaction.direction()).isEqualTo(TransactionDirection.DEBIT);
		assertThat(transaction.description()).isEqualTo("Checkers Centurion");
		assertThat(transaction.occurredAt()).isEqualTo(occurredAt);
		assertThat(transaction.receivedAt()).isEqualTo(FIXED_INSTANT);
		assertThat(transaction.status()).isEqualTo(TransactionStatus.PROCESSED);
		assertThat(transaction.createdAt()).isEqualTo(FIXED_INSTANT);
	}

	@Test
	void registerAllowsNullMerchantIdAndDescription() {
		Transaction transaction = Transaction.register(anId(), UUID.randomUUID(), aSourceId(), "EXT-001", null,
				UUID.randomUUID(), AN_AMOUNT, TransactionDirection.CREDIT, null, FIXED_INSTANT, FIXED_CLOCK);

		assertThat(transaction.merchantId()).isNull();
		assertThat(transaction.description()).isNull();
	}

	@Test
	void registerRejectsNullId() {
		assertThatNullPointerException().isThrownBy(() -> Transaction.register(null, UUID.randomUUID(), aSourceId(),
				"EXT-001", null, UUID.randomUUID(), AN_AMOUNT, TransactionDirection.CREDIT, null, FIXED_INSTANT, FIXED_CLOCK));
	}

	@Test
	void registerRejectsNullCustomerId() {
		assertThatNullPointerException().isThrownBy(() -> Transaction.register(anId(), null, aSourceId(),
				"EXT-001", null, UUID.randomUUID(), AN_AMOUNT, TransactionDirection.CREDIT, null, FIXED_INSTANT, FIXED_CLOCK));
	}

	@Test
	void registerRejectsBlankExternalTransactionId() {
		assertThatIllegalArgumentException().isThrownBy(() -> Transaction.register(anId(), UUID.randomUUID(), aSourceId(),
				" ", null, UUID.randomUUID(), AN_AMOUNT, TransactionDirection.CREDIT, null, FIXED_INSTANT, FIXED_CLOCK));
	}

	@Test
	void registerRejectsExternalTransactionIdTooLong() {
		String tooLong = "X".repeat(151);

		assertThatIllegalArgumentException().isThrownBy(() -> Transaction.register(anId(), UUID.randomUUID(), aSourceId(),
				tooLong, null, UUID.randomUUID(), AN_AMOUNT, TransactionDirection.CREDIT, null, FIXED_INSTANT, FIXED_CLOCK));
	}

	@Test
	void registerRejectsZeroAmount() {
		Money zero = new Money(BigDecimal.ZERO, "ZAR");

		assertThatIllegalArgumentException().isThrownBy(() -> Transaction.register(anId(), UUID.randomUUID(), aSourceId(),
				"EXT-001", null, UUID.randomUUID(), zero, TransactionDirection.CREDIT, null, FIXED_INSTANT, FIXED_CLOCK));
	}

	@Test
	void registerRejectsNegativeAmount() {
		Money negative = new Money(new BigDecimal("-10.00"), "ZAR");

		assertThatIllegalArgumentException().isThrownBy(() -> Transaction.register(anId(), UUID.randomUUID(), aSourceId(),
				"EXT-001", null, UUID.randomUUID(), negative, TransactionDirection.CREDIT, null, FIXED_INSTANT, FIXED_CLOCK));
	}

	@Test
	void registerRejectsDescriptionTooLong() {
		String tooLong = "X".repeat(501);

		assertThatIllegalArgumentException().isThrownBy(() -> Transaction.register(anId(), UUID.randomUUID(), aSourceId(),
				"EXT-001", null, UUID.randomUUID(), AN_AMOUNT, TransactionDirection.CREDIT, tooLong, FIXED_INSTANT, FIXED_CLOCK));
	}

	@Test
	void registerRejectsOccurredAtUnreasonablyFarInTheFuture() {
		Instant tooFarFuture = FIXED_INSTANT.plus(Duration.ofDays(2));

		assertThatIllegalArgumentException().isThrownBy(() -> Transaction.register(anId(), UUID.randomUUID(), aSourceId(),
				"EXT-001", null, UUID.randomUUID(), AN_AMOUNT, TransactionDirection.CREDIT, null, tooFarFuture, FIXED_CLOCK));
	}

	@Test
	void registerAllowsOccurredAtWithinTolerance() {
		Instant slightlyFuture = FIXED_INSTANT.plusSeconds(60);

		Transaction transaction = Transaction.register(anId(), UUID.randomUUID(), aSourceId(), "EXT-001", null,
				UUID.randomUUID(), AN_AMOUNT, TransactionDirection.CREDIT, null, slightlyFuture, FIXED_CLOCK);

		assertThat(transaction.occurredAt()).isEqualTo(slightlyFuture);
	}

	@Test
	void equalityIsByIdOnly() {
		TransactionId id = anId();
		Transaction first = Transaction.register(id, UUID.randomUUID(), aSourceId(), "EXT-001", null,
				UUID.randomUUID(), AN_AMOUNT, TransactionDirection.CREDIT, null, FIXED_INSTANT, FIXED_CLOCK);
		Transaction second = Transaction.reconstitute(id, UUID.randomUUID(), aSourceId(), "EXT-002", null,
				UUID.randomUUID(), AN_AMOUNT, TransactionDirection.DEBIT, null, FIXED_INSTANT, FIXED_INSTANT,
				TransactionStatus.PROCESSED, FIXED_INSTANT);

		assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
	}

	@Test
	void reconstituteRebuildsWithoutRunningRegistrationInvariants() {
		Transaction transaction = Transaction.reconstitute(anId(), UUID.randomUUID(), aSourceId(), "EXT-001", null,
				UUID.randomUUID(), AN_AMOUNT, TransactionDirection.CREDIT, null, FIXED_INSTANT, FIXED_INSTANT,
				TransactionStatus.REJECTED, FIXED_INSTANT);

		assertThat(transaction.status()).isEqualTo(TransactionStatus.REJECTED);
	}

	@Test
	void hasNoMutationMethods() {
		assertThat(Transaction.class.getDeclaredMethods())
				.extracting(java.lang.reflect.Method::getName)
				.doesNotContain("setStatus", "setDescription", "update", "reject");
	}

}
