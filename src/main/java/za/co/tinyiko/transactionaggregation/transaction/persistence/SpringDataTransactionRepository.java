package za.co.tinyiko.transactionaggregation.transaction.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataTransactionRepository extends JpaRepository<TransactionEntity, UUID> {

	Optional<TransactionEntity> findByTransactionSourceIdAndExternalTransactionId(
			UUID transactionSourceId, String externalTransactionId);

}
