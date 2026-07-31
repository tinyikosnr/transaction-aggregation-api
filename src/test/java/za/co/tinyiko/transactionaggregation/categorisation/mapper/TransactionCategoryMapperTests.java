package za.co.tinyiko.transactionaggregation.categorisation.mapper;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategory;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategoryId;
import za.co.tinyiko.transactionaggregation.categorisation.persistence.TransactionCategoryEntity;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionCategoryMapperTests {

	@Test
	void toDomainMapsAllFields() {
		TransactionCategoryEntity entity = new TransactionCategoryEntity();
		UUID id = UUID.randomUUID();
		entity.setId(id);
		entity.setCode("GROCERIES");
		entity.setName("Groceries");
		entity.setDescription("Food and household");
		entity.setFallback(false);
		entity.setActive(true);
		entity.setCreatedAt(Instant.parse("2026-07-31T08:15:00Z"));
		entity.setUpdatedAt(Instant.parse("2026-07-31T09:00:00Z"));

		TransactionCategory category = TransactionCategoryMapper.toDomain(entity);

		assertThat(category.id()).isEqualTo(new TransactionCategoryId(id));
		assertThat(category.code()).isEqualTo("GROCERIES");
		assertThat(category.name()).isEqualTo("Groceries");
		assertThat(category.description()).isEqualTo("Food and household");
		assertThat(category.fallback()).isFalse();
		assertThat(category.active()).isTrue();
		assertThat(category.createdAt()).isEqualTo(Instant.parse("2026-07-31T08:15:00Z"));
		assertThat(category.updatedAt()).isEqualTo(Instant.parse("2026-07-31T09:00:00Z"));
	}

	@Test
	void toDomainMapsNullDescription() {
		TransactionCategoryEntity entity = new TransactionCategoryEntity();
		entity.setId(UUID.randomUUID());
		entity.setCode("UNCATEGORISED");
		entity.setName("Uncategorised");
		entity.setDescription(null);
		entity.setFallback(true);
		entity.setActive(true);
		entity.setCreatedAt(Instant.parse("2026-07-31T08:15:00Z"));
		entity.setUpdatedAt(Instant.parse("2026-07-31T08:15:00Z"));

		TransactionCategory category = TransactionCategoryMapper.toDomain(entity);

		assertThat(category.description()).isNull();
		assertThat(category.fallback()).isTrue();
	}

}
