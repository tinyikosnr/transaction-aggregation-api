package za.co.tinyiko.transactionaggregation.transaction.persistence;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

/**
 * One small, named {@link Specification} per optional search filter (TDS 31), composed by
 * {@link JpaTransactionSearchRepositoryAdapter} via {@code Specification.allOf(...)}. Every
 * filter is genuinely optional and independently combinable, which is exactly the case
 * Specifications exist for - a hand-written JPQL {@code @Query} with a
 * {@code WHERE (:x IS NULL OR ...)} chain for every combination would be substantially harder to
 * read and maintain for the same result. Returning {@code null} for an absent filter is a
 * documented Spring Data idiom: a {@code null} {@link Specification} composed via
 * {@code Specification.allOf}/{@code where} contributes no restriction.
 *
 * <p>Deliberately does not restrict {@code status} by default (unlike
 * {@code JpaTransactionSummaryRepositoryAdapter}'s aggregate queries, which always filter to
 * {@code PROCESSED}) - {@code status} is one of the documented, client-optional search filters
 * here (TDS 31), not an implicit restriction; nothing documents search defaulting to
 * PROCESSED-only.
 */
final class TransactionSpecifications {

	private TransactionSpecifications() {
	}

	static Specification<TransactionEntity> hasCustomerId(UUID customerId) {
		return customerId == null ? null
				: (root, query, cb) -> cb.equal(root.get("customerId"), customerId);
	}

	static Specification<TransactionEntity> hasTransactionSourceId(UUID transactionSourceId) {
		return transactionSourceId == null ? null
				: (root, query, cb) -> cb.equal(root.get("transactionSourceId"), transactionSourceId);
	}

	static Specification<TransactionEntity> hasCategoryId(UUID categoryId) {
		return categoryId == null ? null
				: (root, query, cb) -> cb.equal(root.get("categoryId"), categoryId);
	}

	static Specification<TransactionEntity> hasMerchantId(UUID merchantId) {
		return merchantId == null ? null
				: (root, query, cb) -> cb.equal(root.get("merchantId"), merchantId);
	}

	static Specification<TransactionEntity> hasDirection(String direction) {
		return direction == null ? null
				: (root, query, cb) -> cb.equal(root.get("direction"), direction);
	}

	static Specification<TransactionEntity> hasStatus(String status) {
		return status == null ? null
				: (root, query, cb) -> cb.equal(root.get("status"), status);
	}

	/**
	 * Both bounds inclusive (SAD 34.7's own example range: {@code 00:00:00Z} to {@code 23:59:59Z}
	 * spanning a whole month) - deliberately not aggregation's exclusive-upper-day-boundary
	 * translation, which exists specifically to convert a {@code LocalDate} into an {@code Instant}
	 * range and has no bearing here, where the input is already a precise {@code Instant}. Either
	 * bound may be absent independently, leaving that side of the range open.
	 */
	static Specification<TransactionEntity> occurredBetween(Instant occurredFrom, Instant occurredTo) {
		if (occurredFrom != null && occurredTo != null) {
			return (root, query, cb) -> cb.between(root.get("occurredAt"), occurredFrom, occurredTo);
		}
		if (occurredFrom != null) {
			return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), occurredFrom);
		}
		if (occurredTo != null) {
			return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("occurredAt"), occurredTo);
		}
		return null;
	}

}
