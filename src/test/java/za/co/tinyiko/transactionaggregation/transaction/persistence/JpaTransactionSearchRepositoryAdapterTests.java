package za.co.tinyiko.transactionaggregation.transaction.persistence;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import jakarta.persistence.EntityManager;

import za.co.tinyiko.transactionaggregation.TestcontainersConfiguration;
import za.co.tinyiko.transactionaggregation.transaction.domain.Money;
import za.co.tinyiko.transactionaggregation.transaction.domain.Transaction;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionDirection;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionId;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionSourceId;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionDetailRow;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSearchPage;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSearchQuery;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link JpaTransactionSearchRepositoryAdapter} against real PostgreSQL via
 * Testcontainers - query correctness, paging, sorting, and the {@code findDetailById}
 * intra-module source-code join, none of which a mocked-port unit test can prove.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaTransactionSearchRepositoryAdapterTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC);

	@Autowired
	private SpringDataTransactionRepository springDataTransactionRepository;

	@Autowired
	private SpringDataTransactionSearchRepository springDataTransactionSearchRepository;

	@Autowired
	private EntityManager entityManager;

	private JpaTransactionRepositoryAdapter writeAdapter;
	private JpaTransactionSearchRepositoryAdapter searchAdapter;
	private UUID customerId;
	private UUID otherCustomerId;
	private UUID sourceId;
	private UUID groceriesId;
	private UUID fuelId;
	private UUID checkersId;
	private UUID shellId;

	@BeforeEach
	void setUp() {
		writeAdapter = new JpaTransactionRepositoryAdapter(springDataTransactionRepository);
		searchAdapter = new JpaTransactionSearchRepositoryAdapter(springDataTransactionSearchRepository);

		customerId = insertCustomer();
		otherCustomerId = insertCustomer();
		sourceId = seededId("transaction_sources", "code", "MOCK_BANK_A");
		groceriesId = seededId("transaction_categories", "code", "GROCERIES");
		fuelId = seededId("transaction_categories", "code", "FUEL");
		checkersId = insertMerchant("CHECKERS", "Checkers");
		shellId = insertMerchant("SHELL", "Shell");

		saveTransaction("EXT-A", groceriesId, checkersId, "100.00", TransactionDirection.DEBIT, Instant.parse("2026-01-01T00:00:00Z"));
		saveTransaction("EXT-B", fuelId, shellId, "50.00", TransactionDirection.DEBIT, Instant.parse("2026-01-15T00:00:00Z"));
		saveTransaction("EXT-C", groceriesId, null, "30.00", TransactionDirection.CREDIT, Instant.parse("2026-01-31T23:59:59Z"));
	}

	private UUID insertCustomer() {
		UUID id = UUID.randomUUID();
		entityManager.createNativeQuery(
				"INSERT INTO customers (id, external_reference, first_name, last_name, status, created_at, updated_at, version) "
						+ "VALUES (?1, ?2, 'Jane', 'Doe', 'ACTIVE', now(), now(), 0)")
				.setParameter(1, id)
				.setParameter(2, "EXT-CUST-" + id)
				.executeUpdate();
		return id;
	}

	private UUID insertMerchant(String normalisedName, String displayName) {
		UUID id = UUID.randomUUID();
		entityManager.createNativeQuery(
				"INSERT INTO merchants (id, normalised_name, display_name, created_at, updated_at, version) "
						+ "VALUES (?1, ?2, ?3, now(), now(), 0)")
				.setParameter(1, id)
				.setParameter(2, normalisedName)
				.setParameter(3, displayName)
				.executeUpdate();
		return id;
	}

	private UUID seededId(String table, String column, String value) {
		return (UUID) entityManager.createNativeQuery("SELECT id FROM " + table + " WHERE " + column + " = ?1")
				.setParameter(1, value)
				.getSingleResult();
	}

	private void saveTransaction(String externalId, UUID categoryId, UUID merchantId, String amount, TransactionDirection direction, Instant occurredAt) {
		Transaction transaction = Transaction.register(TransactionId.generate(), customerId, new TransactionSourceId(sourceId),
				externalId, merchantId, categoryId, new Money(new BigDecimal(amount), "ZAR"), direction, "desc", occurredAt, FIXED_CLOCK);
		writeAdapter.save(transaction);
	}

	private static TransactionSearchQuery emptyQuery(int page, int size, boolean ascending) {
		return new TransactionSearchQuery(null, null, null, null, null, null, null, null, page, size, ascending);
	}

	@Test
	void searchWithNoFiltersReturnsEveryRowForTheDatabase() {
		TransactionSearchPage page = searchAdapter.search(emptyQuery(0, 20, false));

		assertThat(page.totalElements()).isEqualTo(3);
		assertThat(page.rows()).hasSize(3);
	}

	@Test
	void filtersByCustomerId() {
		TransactionSearchQuery query = new TransactionSearchQuery(otherCustomerId, null, null, null, null, null, null, null, 0, 20, false);

		TransactionSearchPage page = searchAdapter.search(query);

		assertThat(page.rows()).isEmpty();
		assertThat(page.totalElements()).isZero();
	}

	@Test
	void filtersByCategoryId() {
		TransactionSearchQuery query = new TransactionSearchQuery(null, null, fuelId, null, null, null, null, null, 0, 20, false);

		TransactionSearchPage page = searchAdapter.search(query);

		assertThat(page.rows()).hasSize(1);
		assertThat(page.rows().get(0).categoryId()).isEqualTo(fuelId);
	}

	@Test
	void filtersByMerchantIdExcludingNullMerchantRows() {
		TransactionSearchQuery query = new TransactionSearchQuery(null, null, null, checkersId, null, null, null, null, 0, 20, false);

		TransactionSearchPage page = searchAdapter.search(query);

		assertThat(page.rows()).hasSize(1);
		assertThat(page.rows().get(0).merchantId()).isEqualTo(checkersId);
	}

	@Test
	void filtersByDirection() {
		TransactionSearchQuery query = new TransactionSearchQuery(null, null, null, null, "CREDIT", null, null, null, 0, 20, false);

		TransactionSearchPage page = searchAdapter.search(query);

		assertThat(page.rows()).hasSize(1);
		assertThat(page.rows().get(0).direction()).isEqualTo("CREDIT");
	}

	@Test
	void filtersByStatus() {
		TransactionSearchQuery query = new TransactionSearchQuery(null, null, null, null, null, "PROCESSED", null, null, 0, 20, false);

		TransactionSearchPage page = searchAdapter.search(query);

		assertThat(page.rows()).hasSize(3);
	}

	@Test
	void filtersByOccurredAtRangeInclusiveOnBothBoundaries() {
		TransactionSearchQuery query = new TransactionSearchQuery(null, null, null, null, null, null,
				Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-15T00:00:00Z"), 0, 20, false);

		TransactionSearchPage page = searchAdapter.search(query);

		assertThat(page.rows()).hasSize(2);
	}

	@Test
	void combinesMultipleFiltersWithAnd() {
		TransactionSearchQuery query = new TransactionSearchQuery(customerId, null, groceriesId, checkersId, "DEBIT", null, null, null, 0, 20, false);

		TransactionSearchPage page = searchAdapter.search(query);

		assertThat(page.rows()).hasSize(1);
	}

	@Test
	void paginatesCorrectlyAcrossMultiplePages() {
		TransactionSearchPage firstPage = searchAdapter.search(emptyQuery(0, 2, false));
		TransactionSearchPage secondPage = searchAdapter.search(emptyQuery(1, 2, false));

		assertThat(firstPage.rows()).hasSize(2);
		assertThat(secondPage.rows()).hasSize(1);
		assertThat(firstPage.totalElements()).isEqualTo(3);
		assertThat(secondPage.totalElements()).isEqualTo(3);
	}

	@Test
	void sortsDescendingByOccurredAtByDefault() {
		TransactionSearchPage page = searchAdapter.search(emptyQuery(0, 20, false));

		assertThat(page.rows()).extracting(row -> row.occurredAt())
				.isSortedAccordingTo((a, b) -> b.compareTo(a));
	}

	@Test
	void sortsAscendingByOccurredAtWhenRequested() {
		TransactionSearchPage page = searchAdapter.search(emptyQuery(0, 20, true));

		assertThat(page.rows()).extracting(row -> row.occurredAt())
				.isSortedAccordingTo(Instant::compareTo);
	}

	@Test
	void returnsEmptyPageWhenNothingMatches() {
		TransactionSearchQuery query = new TransactionSearchQuery(null, null, null, null, "CREDIT", null, null, null, 0, 20, false);
		TransactionSearchQuery contradictory = new TransactionSearchQuery(otherCustomerId, null, null, null, null, null, null, null, 0, 20, false);

		assertThat(searchAdapter.search(contradictory).rows()).isEmpty();
	}

	@Test
	void findDetailByIdJoinsTheSourceCodeFromTheSameModulesOwnTable() {
		Transaction saved = writeAdapter.findBySourceIdAndExternalTransactionId(new TransactionSourceId(sourceId), "EXT-A").orElseThrow();

		Optional<TransactionDetailRow> detail = searchAdapter.findDetailById(saved.id().value());

		assertThat(detail).isPresent();
		assertThat(detail.get().sourceCode()).isEqualTo("MOCK_BANK_A");
		assertThat(detail.get().merchantId()).isEqualTo(checkersId);
		assertThat(detail.get().categoryId()).isEqualTo(groceriesId);
	}

	@Test
	void findDetailByIdReturnsEmptyForAnUnknownId() {
		assertThat(searchAdapter.findDetailById(UUID.randomUUID())).isEmpty();
	}

	@Test
	void unresolvableFilterIdsProduceCorrectlyEmptyResultsRatherThanMatchingEverything() {
		TransactionSearchQuery query = new TransactionSearchQuery(null, null, UUID.randomUUID(), null, null, null, null, null, 0, 20, false);

		assertThat(searchAdapter.search(query).rows()).isEmpty();
	}

}
