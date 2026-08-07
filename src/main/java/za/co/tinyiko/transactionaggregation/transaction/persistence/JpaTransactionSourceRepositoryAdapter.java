package za.co.tinyiko.transactionaggregation.transaction.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionSource;
import za.co.tinyiko.transactionaggregation.transaction.mapper.TransactionSourceMapper;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSourceRepositoryPort;

@Component
class JpaTransactionSourceRepositoryAdapter implements TransactionSourceRepositoryPort {

	private final SpringDataTransactionSourceRepository springDataTransactionSourceRepository;

	JpaTransactionSourceRepositoryAdapter(SpringDataTransactionSourceRepository springDataTransactionSourceRepository) {
		this.springDataTransactionSourceRepository = springDataTransactionSourceRepository;
	}

	@Override
	public Optional<TransactionSource> findByCode(String code) {
		return springDataTransactionSourceRepository.findByCode(code)
				.map(TransactionSourceMapper::toDomain);
	}

}
