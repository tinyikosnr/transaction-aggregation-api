package za.co.tinyiko.transactionaggregation.transaction.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataTransactionSourceRepository extends JpaRepository<TransactionSourceEntity, UUID> {

	Optional<TransactionSourceEntity> findByCode(String code);

}
