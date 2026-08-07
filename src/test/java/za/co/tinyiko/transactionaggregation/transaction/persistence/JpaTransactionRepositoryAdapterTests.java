package za.co.tinyiko.transactionaggregation.transaction.persistence;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import jakarta.persistence.EntityManager;

import za.co.tinyiko.transactionaggregation.TestcontainersConfiguration;
import za.co.tinyiko.transactionaggregation.transaction.application.DuplicateTransactionException;
import za.co.tinyiko.transactionaggregation.transaction.domain.Money;
import za.co.tinyiko.transactionaggregation.transaction.domain.Transaction;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionDirection;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionId;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionSourceId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the port -> adapter -> JPA -> Testcontainers-PostgreSQL chain, including the V9
 * migration Flyway applies at context startup. {@code transactions} has real foreign keys to
 * {@code customers}, {@code transaction_sources} and {@code transaction_categories}, so fixture
 * rows are set up via {@link EntityManager} native queries rather than importing another
 * module's Spring Data repository into this test - {@code customers} needs a fresh row (nothing
 * seeds customer data), while {@code transaction_sources}/{@code transaction_categories} reuse
 * the already-seeded V10/V5 rows.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaTransactionRepositoryAdapterTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-06T08:15:00Z"), ZoneOffset.UTC);

	@Autowired
	private SpringDataTransactionRepository springDataTransactionRepository;

	@Autowired
	private EntityManager entityManager;

	private JpaTransactionRepositoryAdapter adapter;
	private UUID customerId;
	private UUID sourceId;
	private UUID categoryId;

	@BeforeEach
	void setUp() {
		adapter = new JpaTransactionRepositoryAdapter(springDataTransactionRepository);
		customerId = insertCustomer();
		sourceId = seededId("transaction_sources", "MOCK_BANK_A");
		categoryId = seededId("transaction_categories", "UNCATEGORISED");
	}

	private UUID insertCustomer() {
		UUID id = UUID.randomUUID();
		entityManager.createNativeQuery(
				"INSERT INTO customers (id, external_reference, first_name, last_name, status, created_at, updated_at, version) "
						+ "VALUES (?1, ?2, 'Jane', 'Doe', 'ACTIVE', now(), now(), 0)")
				.setParameter(1, id)
				.setParameter(2, "EXT-" + id)
				.executeUpdate();
		return id;
	}

	private UUID seededId(String table, String code) {
		return (UUID) entityManager.createNativeQuery("SELECT id FROM " + table + " WHERE code = ?1")
				.setParameter(1, code)
				.getSingleResult();
	}

	private Transaction aTransaction(String externalTransactionId) {
		return Transaction.register(TransactionId.generate(), customerId, new TransactionSourceId(sourceId),
				externalTransactionId, null, categoryId, new Money(new BigDecimal("125.50"), "ZAR"),
				TransactionDirection.DEBIT, "Checkers Centurion", Instant.parse("2026-08-06T08:00:00Z"), FIXED_CLOCK);
	}

	@Test
	void savePersistsAndFindBySourceAndExternalIdReturnsIt() {
		Transaction transaction = aTransaction("EXT-001");

		Transaction saved = adapter.save(transaction);

		assertThat(saved.externalTransactionId()).isEqualTo("EXT-001");
		assertThat(adapter.findBySourceIdAndExternalTransactionId(new TransactionSourceId(sourceId), "EXT-001"))
				.contains(saved);
	}

	@Test
	void findBySourceAndExternalIdReturnsEmptyWhenAbsent() {
		assertThat(adapter.findBySourceIdAndExternalTransactionId(new TransactionSourceId(sourceId), "NO-SUCH-ID"))
				.isEmpty();
	}

	@Test
	void duplicateSourceAndExternalIdIsTranslatedIntoADomainException() {
		adapter.save(aTransaction("EXT-DUP"));
		Transaction duplicate = aTransaction("EXT-DUP");

		assertThatThrownBy(() -> adapter.save(duplicate))
				.isInstanceOf(DuplicateTransactionException.class);
	}

}
