package za.co.tinyiko.transactionaggregation.transaction.mapper;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.transaction.domain.SourceStatus;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionSource;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionSourceId;
import za.co.tinyiko.transactionaggregation.transaction.persistence.TransactionSourceEntity;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionSourceMapperTests {

	@Test
	void toDomainMapsAllFields() {
		TransactionSourceEntity entity = new TransactionSourceEntity();
		UUID id = UUID.randomUUID();
		entity.setId(id);
		entity.setCode("MOCK_BANK_A");
		entity.setName("Mock Bank A");
		entity.setStatus("ACTIVE");
		entity.setCreatedAt(Instant.parse("2026-08-06T08:15:00Z"));
		entity.setUpdatedAt(Instant.parse("2026-08-06T09:00:00Z"));

		TransactionSource source = TransactionSourceMapper.toDomain(entity);

		assertThat(source.id()).isEqualTo(new TransactionSourceId(id));
		assertThat(source.code()).isEqualTo("MOCK_BANK_A");
		assertThat(source.name()).isEqualTo("Mock Bank A");
		assertThat(source.status()).isEqualTo(SourceStatus.ACTIVE);
		assertThat(source.createdAt()).isEqualTo(Instant.parse("2026-08-06T08:15:00Z"));
		assertThat(source.updatedAt()).isEqualTo(Instant.parse("2026-08-06T09:00:00Z"));
	}

	@Test
	void toDomainMapsInactiveStatus() {
		TransactionSourceEntity entity = new TransactionSourceEntity();
		entity.setId(UUID.randomUUID());
		entity.setCode("MANUAL_UPLOAD");
		entity.setName("Manual Upload");
		entity.setStatus("INACTIVE");
		entity.setCreatedAt(Instant.parse("2026-08-06T08:15:00Z"));
		entity.setUpdatedAt(Instant.parse("2026-08-06T08:15:00Z"));

		TransactionSource source = TransactionSourceMapper.toDomain(entity);

		assertThat(source.status()).isEqualTo(SourceStatus.INACTIVE);
		assertThat(source.isActive()).isFalse();
	}

}
