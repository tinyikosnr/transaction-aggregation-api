package za.co.tinyiko.transactionaggregation.categorisation.mapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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

	@Test
	void toEntityMapsAllFieldsIncludingTimestamps() {
		Clock clock = Clock.fixed(Instant.parse("2026-08-10T08:15:00Z"), ZoneOffset.UTC);
		CategorisationRule rule = CategorisationRule.register(CategorisationRuleId.generate(), TransactionCategoryId.generate(),
				MatchField.MERCHANT, MatchOperator.CONTAINS, "SHELL", Direction.DEBIT, 30, clock);

		CategorisationRuleEntity entity = CategorisationRuleMapper.toEntity(rule);

		assertThat(entity.getId()).isEqualTo(rule.id().value());
		assertThat(entity.getCategoryId()).isEqualTo(rule.categoryId().value());
		assertThat(entity.getMatchField()).isEqualTo(MatchField.MERCHANT);
		assertThat(entity.getOperator()).isEqualTo(MatchOperator.CONTAINS);
		assertThat(entity.getMatchValue()).isEqualTo("SHELL");
		assertThat(entity.getDirection()).isEqualTo(Direction.DEBIT);
		assertThat(entity.getPriority()).isEqualTo(30);
		assertThat(entity.isActive()).isTrue();
		assertThat(entity.getCreatedAt()).isEqualTo(Instant.parse("2026-08-10T08:15:00Z"));
		assertThat(entity.getUpdatedAt()).isEqualTo(Instant.parse("2026-08-10T08:15:00Z"));
	}

	@Test
	void applyToMutatesTheExistingEntityInPlaceWithoutTouchingCreatedAt() {
		CategorisationRuleEntity entity = new CategorisationRuleEntity();
		Instant originalCreatedAt = Instant.parse("2026-01-01T00:00:00Z");
		entity.setId(UUID.randomUUID());
		entity.setCategoryId(UUID.randomUUID());
		entity.setMatchField(MatchField.MERCHANT);
		entity.setOperator(MatchOperator.CONTAINS);
		entity.setMatchValue("OLD");
		entity.setDirection(Direction.DEBIT);
		entity.setPriority(10);
		entity.setActive(true);
		entity.setCreatedAt(originalCreatedAt);
		entity.setUpdatedAt(originalCreatedAt);

		Clock updateClock = Clock.fixed(Instant.parse("2026-08-10T09:00:00Z"), ZoneOffset.UTC);
		CategorisationRule updatedFields = CategorisationRule.update(new CategorisationRuleId(entity.getId()),
				new TransactionCategoryId(entity.getCategoryId()), MatchField.DESCRIPTION, MatchOperator.REGEX,
				"NEW", Direction.CREDIT, 999, false, updateClock);

		CategorisationRuleMapper.applyTo(entity, updatedFields);

		assertThat(entity.getMatchField()).isEqualTo(MatchField.DESCRIPTION);
		assertThat(entity.getOperator()).isEqualTo(MatchOperator.REGEX);
		assertThat(entity.getMatchValue()).isEqualTo("NEW");
		assertThat(entity.getDirection()).isEqualTo(Direction.CREDIT);
		assertThat(entity.getPriority()).isEqualTo(999);
		assertThat(entity.isActive()).isFalse();
		assertThat(entity.getCreatedAt()).isEqualTo(originalCreatedAt);
		assertThat(entity.getUpdatedAt()).isEqualTo(Instant.parse("2026-08-10T09:00:00Z"));
	}

}
