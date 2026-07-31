package za.co.tinyiko.transactionaggregation.categorisation.mapper;

import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRule;
import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRuleId;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategoryId;
import za.co.tinyiko.transactionaggregation.categorisation.persistence.CategorisationRuleEntity;

/**
 * Maps a {@link CategorisationRuleEntity} to the domain {@link CategorisationRule}.
 * {@code toDomain} only, for the same reason as {@link TransactionCategoryMapper}: no
 * application code writes a rule in this branch, only the V6 Flyway seed migration.
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

}
