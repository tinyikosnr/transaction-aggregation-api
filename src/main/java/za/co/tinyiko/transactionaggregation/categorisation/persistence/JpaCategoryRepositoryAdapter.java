package za.co.tinyiko.transactionaggregation.categorisation.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategory;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategoryId;
import za.co.tinyiko.transactionaggregation.categorisation.mapper.TransactionCategoryMapper;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategoryRepositoryPort;

@Component
class JpaCategoryRepositoryAdapter implements CategoryRepositoryPort {

	private final SpringDataTransactionCategoryRepository springDataTransactionCategoryRepository;

	JpaCategoryRepositoryAdapter(SpringDataTransactionCategoryRepository springDataTransactionCategoryRepository) {
		this.springDataTransactionCategoryRepository = springDataTransactionCategoryRepository;
	}

	@Override
	public Optional<TransactionCategory> findFallback() {
		return springDataTransactionCategoryRepository.findFirstByFallbackTrue()
				.map(TransactionCategoryMapper::toDomain);
	}

	@Override
	public Optional<TransactionCategory> findById(TransactionCategoryId id) {
		return springDataTransactionCategoryRepository.findById(id.value())
				.map(TransactionCategoryMapper::toDomain);
	}

	@Override
	public Optional<TransactionCategory> findByCode(String code) {
		return springDataTransactionCategoryRepository.findByCode(code)
				.map(TransactionCategoryMapper::toDomain);
	}

	@Override
	public List<TransactionCategory> findByIds(Collection<TransactionCategoryId> ids) {
		List<UUID> rawIds = ids.stream().map(TransactionCategoryId::value).toList();
		return springDataTransactionCategoryRepository.findAllById(rawIds).stream()
				.map(TransactionCategoryMapper::toDomain)
				.toList();
	}

	@Override
	public List<TransactionCategory> findAll() {
		return springDataTransactionCategoryRepository.findAll().stream()
				.map(TransactionCategoryMapper::toDomain)
				.toList();
	}

}
