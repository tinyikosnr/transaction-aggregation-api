package za.co.tinyiko.transactionaggregation.categorisation.mapper;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRule;
import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRuleId;
import za.co.tinyiko.transactionaggregation.categorisation.domain.Direction;
import za.co.tinyiko.transactionaggregation.categorisation.domain.MatchField;
import za.co.tinyiko.transactionaggregation.categorisation.domain.MatchOperator;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategoryId;
import za.co.tinyiko.transactionaggregation.categorisation.persistence.CategorisationRuleEntity;

import static org.assertj.core.api.Assertions.assertThat;

class CategorisationRuleMapperTests {

	@Test
	void toDomainMapsAllFields() {
		CategorisationRuleEntity entity = new CategorisationRuleEntity();
		UUID id = UUID.randomUUID();
		UUID categoryId = UUID.randomUUID();
		entity.setId(id);
		entity.setCategoryId(categoryId);
		entity.setMatchField(MatchField.MERCHANT);
		entity.setOperator(MatchOperator.CONTAINS);
		entity.setMatchValue("SHELL");
		entity.setDirection(Direction.DEBIT);
		entity.setPriority(30);
		entity.setActive(true);
		entity.setCreatedAt(Instant.parse("2026-07-31T08:15:00Z"));
		entity.setUpdatedAt(Instant.parse("2026-07-31T09:00:00Z"));

		CategorisationRule rule = CategorisationRuleMapper.toDomain(entity);

		assertThat(rule.id()).isEqualTo(new CategorisationRuleId(id));
		assertThat(rule.categoryId()).isEqualTo(new TransactionCategoryId(categoryId));
		assertThat(rule.matchField()).isEqualTo(MatchField.MERCHANT);
		assertThat(rule.operator()).isEqualTo(MatchOperator.CONTAINS);
		assertThat(rule.matchValue()).isEqualTo("SHELL");
		assertThat(rule.direction()).isEqualTo(Direction.DEBIT);
		assertThat(rule.priority()).isEqualTo(30);
		assertThat(rule.active()).isTrue();
		assertThat(rule.createdAt()).isEqualTo(Instant.parse("2026-07-31T08:15:00Z"));
		assertThat(rule.updatedAt()).isEqualTo(Instant.parse("2026-07-31T09:00:00Z"));
	}

	@Test
	void toDomainMapsInactiveRule() {
		CategorisationRuleEntity entity = new CategorisationRuleEntity();
		entity.setId(UUID.randomUUID());
		entity.setCategoryId(UUID.randomUUID());
		entity.setMatchField(MatchField.DESCRIPTION);
		entity.setOperator(MatchOperator.REGEX);
		entity.setMatchValue(".*");
		entity.setDirection(Direction.CREDIT);
		entity.setPriority(999);
		entity.setActive(false);
		entity.setCreatedAt(Instant.parse("2026-07-31T08:15:00Z"));
		entity.setUpdatedAt(Instant.parse("2026-07-31T08:15:00Z"));

		CategorisationRule rule = CategorisationRuleMapper.toDomain(entity);

		assertThat(rule.active()).isFalse();
		assertThat(rule.matches(null, "anything", Direction.CREDIT)).isTrue();
	}

}
