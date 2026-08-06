package za.co.tinyiko.transactionaggregation.categorisation.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategory;
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

}
