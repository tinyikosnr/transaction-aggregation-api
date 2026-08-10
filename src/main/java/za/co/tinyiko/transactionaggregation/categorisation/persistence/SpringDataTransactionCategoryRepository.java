package za.co.tinyiko.transactionaggregation.categorisation.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataTransactionCategoryRepository extends JpaRepository<TransactionCategoryEntity, UUID> {

	Optional<TransactionCategoryEntity> findFirstByFallbackTrue();

	Optional<TransactionCategoryEntity> findByCode(String code);

}
