package za.co.tinyiko.transactionaggregation.categorisation.mapper;

import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRule;
import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRuleId;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategoryId;
import za.co.tinyiko.transactionaggregation.categorisation.persistence.CategorisationRuleEntity;

/**
 * Maps between a {@link CategorisationRuleEntity} and the domain {@link CategorisationRule}.
 * {@code toDomain} was the only direction before {@code feature/category-admin}, since nothing
 * wrote a rule through application code; that branch adds {@code toEntity} (create - a brand new
 * row, no pre-existing managed entity to mutate) and {@code applyTo} (update - mutates an
 * already-loaded, managed entity in place so Hibernate's own dirty-checking and {@code @Version}
 * increment stay correct, the same convention every other mutable aggregate's mapper in this
 * codebase already follows).
 */
public final class CategorisationRuleMapper {

	private CategorisationRuleMapper() {
	}

	public static CategorisationRule toDomain(CategorisationRuleEntity entity) {
		return CategorisationRule.reconstitute(
				new CategorisationRuleId(entity.getId()),
				new TransactionCategoryId(entity.getCategoryId()),
				entity.getMatchField(),
				entity.getOperator(),
				entity.getMatchValue(),
				entity.getDirection(),
				entity.getPriority(),
				entity.isActive(),
				entity.getCreatedAt(),
				entity.getUpdatedAt());
	}

	public static CategorisationRuleEntity toEntity(CategorisationRule rule) {
		CategorisationRuleEntity entity = new CategorisationRuleEntity();
		entity.setId(rule.id().value());
		applyMutableFields(entity, rule);
		entity.setCreatedAt(rule.createdAt());
		entity.setUpdatedAt(rule.updatedAt());
		return entity;
	}

	public static void applyTo(CategorisationRuleEntity entity, CategorisationRule rule) {
		applyMutableFields(entity, rule);
		entity.setUpdatedAt(rule.updatedAt());
	}

	private static void applyMutableFields(CategorisationRuleEntity entity, CategorisationRule rule) {
		entity.setCategoryId(rule.categoryId().value());
		entity.setMatchField(rule.matchField());
		entity.setOperator(rule.operator());
		entity.setMatchValue(rule.matchValue());
		entity.setDirection(rule.direction());
		entity.setPriority(rule.priority());
		entity.setActive(rule.active());
	}

}
