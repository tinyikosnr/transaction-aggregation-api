package za.co.tinyiko.transactionaggregation.categorisation.mapper;

import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategory;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategoryId;
import za.co.tinyiko.transactionaggregation.categorisation.persistence.TransactionCategoryEntity;

/**
 * Maps a {@link TransactionCategoryEntity} to the domain {@link TransactionCategory}.
 * {@code toDomain} only: nothing in this branch writes a category through application code
 * (see {@link TransactionCategoryEntity}'s Javadoc), so there is no {@code applyTo} counterpart.
 */
public final class TransactionCategoryMapper {

	private TransactionCategoryMapper() {
	}

	public static TransactionCategory toDomain(TransactionCategoryEntity entity) {
		return TransactionCategory.reconstitute(
				new TransactionCategoryId(entity.getId()),
				entity.getCode(),
				entity.getName(),
				entity.getDescription(),
				entity.isFallback(),
				entity.isActive(),
				entity.getCreatedAt(),
				entity.getUpdatedAt());
	}

}
