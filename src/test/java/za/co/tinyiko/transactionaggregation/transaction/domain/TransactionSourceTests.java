package za.co.tinyiko.transactionaggregation.transaction.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class TransactionSourceTests {

	private static final Instant FIXED_INSTANT = Instant.parse("2026-08-06T08:15:00Z");
	private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

	private static TransactionSourceId anId() {
		return TransactionSourceId.generate();
	}

	@Test
	void registerCreatesSourceWithTimestamps() {
		TransactionSourceId id = anId();

		TransactionSource source = TransactionSource.register(id, "MOCK_BANK_A", "Mock Bank A", SourceStatus.ACTIVE, FIXED_CLOCK);

		assertThat(source.id()).isEqualTo(id);
		assertThat(source.code()).isEqualTo("MOCK_BANK_A");
		assertThat(source.name()).isEqualTo("Mock Bank A");
		assertThat(source.status()).isEqualTo(SourceStatus.ACTIVE);
		assertThat(source.isActive()).isTrue();
		assertThat(source.createdAt()).isEqualTo(FIXED_INSTANT);
		assertThat(source.updatedAt()).isEqualTo(FIXED_INSTANT);
	}

	@Test
	void isActiveIsFalseWhenInactive() {
		TransactionSource source = TransactionSource.register(anId(), "MOCK_BANK_A", "Mock Bank A", SourceStatus.INACTIVE, FIXED_CLOCK);

		assertThat(source.isActive()).isFalse();
	}

	@Test
	void registerRejectsNullId() {
		assertThatNullPointerException()
				.isThrownBy(() -> TransactionSource.register(null, "MOCK_BANK_A", "Mock Bank A", SourceStatus.ACTIVE, FIXED_CLOCK));
	}

	@Test
	void registerRejectsBlankCode() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> TransactionSource.register(anId(), " ", "Mock Bank A", SourceStatus.ACTIVE, FIXED_CLOCK));
	}

	@Test
	void registerRejectsCodeTooLong() {
		String tooLong = "X".repeat(51);

		assertThatIllegalArgumentException()
				.isThrownBy(() -> TransactionSource.register(anId(), tooLong, "Mock Bank A", SourceStatus.ACTIVE, FIXED_CLOCK));
	}

	@Test
	void equalityIsByIdOnly() {
		TransactionSourceId id = anId();
		TransactionSource first = TransactionSource.register(id, "MOCK_BANK_A", "Mock Bank A", SourceStatus.ACTIVE, FIXED_CLOCK);
		TransactionSource second = TransactionSource.reconstitute(id, "DIFFERENT", "Different", SourceStatus.INACTIVE, FIXED_INSTANT, FIXED_INSTANT);

		assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
	}

}
