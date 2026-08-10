package za.co.tinyiko.transactionaggregation.transaction.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import za.co.tinyiko.transactionaggregation.transaction.port.TransactionDetailRow;

/**
 * Separate from {@code SpringDataTransactionRepository} (write + duplicate-check) and
 * {@code SpringDataTransactionSummaryRepository} (aggregate reads) - mirrors the same
 * one-Spring-Data-interface-per-outbound-port precedent already established for those two.
 * {@link JpaSpecificationExecutor} backs the search side (see {@link TransactionSpecifications});
 * {@link #findDetailById} is a plain JPQL constructor-expression query joining
 * {@code transaction_sources} - an intra-module join ({@code transaction} owns both tables), not
 * a mapped JPA association (this codebase deliberately keeps entities relationship-free), hence
 * the classic implicit comma-join correlated by {@code WHERE}.
 */
interface SpringDataTransactionSearchRepository extends JpaRepository<TransactionEntity, UUID>, JpaSpecificationExecutor<TransactionEntity> {

	@Query("""
			SELECT new za.co.tinyiko.transactionaggregation.transaction.port.TransactionDetailRow(
				t.id, t.customerId, s.code, t.externalTransactionId, t.merchantId, t.categoryId,
				t.amount, t.currency, t.direction, t.description, t.occurredAt, t.receivedAt, t.status, t.createdAt)
			FROM TransactionEntity t, TransactionSourceEntity s
			WHERE t.id = :id AND s.id = t.transactionSourceId
			""")
	Optional<TransactionDetailRow> findDetailById(@Param("id") UUID id);

}
