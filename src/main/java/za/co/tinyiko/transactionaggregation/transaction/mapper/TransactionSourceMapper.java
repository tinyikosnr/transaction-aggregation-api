package za.co.tinyiko.transactionaggregation.transaction.mapper;

import za.co.tinyiko.transactionaggregation.transaction.domain.SourceStatus;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionSource;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionSourceId;
import za.co.tinyiko.transactionaggregation.transaction.persistence.TransactionSourceEntity;

/**
 * Maps a {@link TransactionSourceEntity} to the domain {@link TransactionSource}. {@code
 * toDomain} only: nothing in this branch writes a source through application code, only the
 * V10 Flyway seed migration.
 */
public final class TransactionSourceMapper {

	private TransactionSourceMapper() {
	}

	public static TransactionSource toDomain(TransactionSourceEntity entity) {
		return TransactionSource.reconstitute(
				new TransactionSourceId(entity.getId()),
				entity.getCode(),
				entity.getName(),
				SourceStatus.valueOf(entity.getStatus()),
				entity.getCreatedAt(),
				entity.getUpdatedAt());
	}

}
