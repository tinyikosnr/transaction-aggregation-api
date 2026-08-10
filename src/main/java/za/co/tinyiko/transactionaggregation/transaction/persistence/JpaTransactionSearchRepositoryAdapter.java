package za.co.tinyiko.transactionaggregation.transaction.persistence;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import za.co.tinyiko.transactionaggregation.transaction.port.TransactionDetailRow;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSearchPage;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSearchQuery;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSearchRepositoryPort;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSearchRow;

/**
 * {@code Specification}/{@code Pageable}/{@code Page} never leave this class - every method
 * returns {@code transaction.port}'s own primitive-based row/page types, matching the same
 * boundary discipline already applied to {@code JpaTransactionSummaryRepositoryAdapter}.
 * {@code findAll(spec, pageable)} necessarily loads full {@link TransactionEntity} rows (Spring
 * Data Specifications don't support constructor-expression projections the way plain JPQL
 * {@code @Query} does), but the resulting entities never cross the port boundary - they are
 * mapped to the lean {@link TransactionSearchRow} in memory, on rows already fetched by the one
 * query, before returning.
 */
@Component
class JpaTransactionSearchRepositoryAdapter implements TransactionSearchRepositoryPort {

	private final SpringDataTransactionSearchRepository springDataTransactionSearchRepository;

	JpaTransactionSearchRepositoryAdapter(SpringDataTransactionSearchRepository springDataTransactionSearchRepository) {
		this.springDataTransactionSearchRepository = springDataTransactionSearchRepository;
	}

	@Override
	public Optional<TransactionDetailRow> findDetailById(UUID transactionId) {
		return springDataTransactionSearchRepository.findDetailById(transactionId);
	}

	@Override
	public TransactionSearchPage search(TransactionSearchQuery query) {
		Specification<TransactionEntity> specification = combine(query);
		Pageable pageable = PageRequest.of(query.page(), query.size(),
				Sort.by(query.sortAscending() ? Sort.Direction.ASC : Sort.Direction.DESC, "occurredAt"));

		Page<TransactionEntity> page = springDataTransactionSearchRepository.findAll(specification, pageable);

		List<TransactionSearchRow> rows = page.getContent().stream()
				.map(JpaTransactionSearchRepositoryAdapter::toRow)
				.toList();
		return new TransactionSearchPage(rows, page.getTotalElements());
	}

	private static Specification<TransactionEntity> combine(TransactionSearchQuery query) {
		List<Specification<TransactionEntity>> specifications = Stream.of(
				TransactionSpecifications.hasCustomerId(query.customerId()),
				TransactionSpecifications.hasTransactionSourceId(query.transactionSourceId()),
				TransactionSpecifications.hasCategoryId(query.categoryId()),
				TransactionSpecifications.hasMerchantId(query.merchantId()),
				TransactionSpecifications.hasDirection(query.direction()),
				TransactionSpecifications.hasStatus(query.status()),
				TransactionSpecifications.occurredBetween(query.occurredFrom(), query.occurredTo())
		).filter(Objects::nonNull).toList();

		return Specification.allOf(specifications);
	}

	private static TransactionSearchRow toRow(TransactionEntity entity) {
		return new TransactionSearchRow(
				entity.getId(),
				entity.getCustomerId(),
				entity.getMerchantId(),
				entity.getCategoryId(),
				entity.getAmount(),
				entity.getCurrency(),
				entity.getDirection(),
				entity.getOccurredAt());
	}

}
