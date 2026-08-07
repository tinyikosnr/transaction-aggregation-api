package za.co.tinyiko.transactionaggregation.transaction.persistence;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import za.co.tinyiko.transactionaggregation.transaction.application.DuplicateTransactionException;
import za.co.tinyiko.transactionaggregation.transaction.domain.Transaction;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionSourceId;
import za.co.tinyiko.transactionaggregation.transaction.mapper.TransactionMapper;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionRepositoryPort;

@Component
class JpaTransactionRepositoryAdapter implements TransactionRepositoryPort {

	private final SpringDataTransactionRepository springDataTransactionRepository;

	JpaTransactionRepositoryAdapter(SpringDataTransactionRepository springDataTransactionRepository) {
		this.springDataTransactionRepository = springDataTransactionRepository;
	}

	@Override
	public Transaction save(Transaction transaction) {
		TransactionEntity entity = TransactionMapper.toEntity(transaction);
		try {
			// saveAndFlush, not save: the unique-constraint violation must surface here,
			// synchronously - a deferred flush would let it escape uncaught later, outside
			// this method, at transaction commit.
			TransactionEntity saved = springDataTransactionRepository.saveAndFlush(entity);
			return TransactionMapper.toDomain(saved);
		} catch (DataIntegrityViolationException e) {
			throw new DuplicateTransactionException(
					transaction.transactionSourceId().value(), transaction.externalTransactionId(), e);
		}
	}

	@Override
	public Optional<Transaction> findBySourceIdAndExternalTransactionId(
			TransactionSourceId transactionSourceId, String externalTransactionId) {
		return springDataTransactionRepository
				.findByTransactionSourceIdAndExternalTransactionId(transactionSourceId.value(), externalTransactionId)
				.map(TransactionMapper::toDomain);
	}

}
