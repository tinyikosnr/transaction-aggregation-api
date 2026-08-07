package za.co.tinyiko.transactionaggregation.transaction.persistence;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
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
import za.co.tinyiko.transactionaggregation.transaction.port.CategoryTotalsRow;
import za.co.tinyiko.transactionaggregation.transaction.port.CustomerTotalsRow;
import za.co.tinyiko.transactionaggregation.transaction.port.MerchantTotalsRow;
import za.co.tinyiko.transactionaggregation.transaction.port.MonthlyTotalsRow;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the aggregate SQL queries backing {@link TransactionSummaryRepositoryPort} against
 * real PostgreSQL - not just compiled, but actually run, per the explicit instruction to verify
 * JPQL/native aggregate behaviour empirically (month grouping, {@code COALESCE} on empty result
 * sets) rather than assume it.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaTransactionSummaryRepositoryAdapterTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC);
	private static final Instant RANGE_FROM = Instant.parse("2026-01-01T00:00:00Z");
	private static final Instant RANGE_TO_EXCLUSIVE = Instant.parse("2026-03-01T00:00:00Z");

	@Autowired
	private SpringDataTransactionRepository springDataTransactionRepository;

	@Autowired
	private SpringDataTransactionSummaryRepository springDataTransactionSummaryRepository;

	@Autowired
	private EntityManager entityManager;

	private JpaTransactionRepositoryAdapter writeAdapter;
	private JpaTransactionSummaryRepositoryAdapter adapter;
	private UUID customerId;
	private UUID otherCustomerId;
	private UUID sourceId;
	private UUID groceriesId;
	private UUID fuelId;
	private UUID otherIncomeId;
	private UUID checkersId;
	private UUID shellId;

	@BeforeEach
	void setUp() {
		writeAdapter = new JpaTransactionRepositoryAdapter(springDataTransactionRepository);
		adapter = new JpaTransactionSummaryRepositoryAdapter(springDataTransactionSummaryRepository);

		customerId = insertCustomer();
		otherCustomerId = insertCustomer();
		sourceId = seededId("transaction_sources", "code", "MOCK_BANK_A");
		groceriesId = seededId("transaction_categories", "code", "GROCERIES");
		fuelId = seededId("transaction_categories", "code", "FUEL");
		otherIncomeId = seededId("transaction_categories", "code", "OTHER_INCOME");
		checkersId = insertMerchant("CHECKERS", "Checkers");
		shellId = insertMerchant("SHELL", "Shell");

		// In range [2026-01-01, 2026-03-01) - the query range under test.
		saveTransaction("EXT-A", groceriesId, checkersId, "100.00", TransactionDirection.DEBIT, Instant.parse("2026-01-01T00:00:00Z")); // exact lower bound
		saveTransaction("EXT-B", otherIncomeId, null, "5000.00", TransactionDirection.CREDIT, Instant.parse("2026-01-15T12:00:00Z"));
		saveTransaction("EXT-C", fuelId, shellId, "50.00", TransactionDirection.DEBIT, Instant.parse("2026-02-28T23:59:59Z")); // just inside upper bound
		saveTransaction("EXT-D", groceriesId, null, "30.00", TransactionDirection.DEBIT, Instant.parse("2026-02-10T08:00:00Z")); // no merchant

		// Out of range - must never appear in any result.
		saveTransaction("EXT-E", groceriesId, checkersId, "999.00", TransactionDirection.DEBIT, Instant.parse("2026-03-01T00:00:00Z")); // exact exclusive upper bound
		saveTransaction("EXT-F", groceriesId, checkersId, "999.00", TransactionDirection.DEBIT, Instant.parse("2025-12-31T23:59:59Z")); // just before lower bound

		// REJECTED status - no code path currently produces this, inserted directly to prove
		// the defensive status = 'PROCESSED' filter actually excludes it.
		insertRejectedTransaction("EXT-G", groceriesId, checkersId, "999.00", "DEBIT", Instant.parse("2026-01-20T00:00:00Z"));
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

	private void insertRejectedTransaction(String externalId, UUID categoryId, UUID merchantId, String amount, String direction, Instant occurredAt) {
		entityManager.createNativeQuery(
				"INSERT INTO transactions (id, customer_id, transaction_source_id, external_transaction_id, merchant_id, "
						+ "category_id, amount, currency, direction, description, occurred_at, received_at, status, created_at, version) "
						+ "VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, 'ZAR', ?8, 'rejected fixture', ?9, ?9, 'REJECTED', ?9, 0)")
				.setParameter(1, UUID.randomUUID())
				.setParameter(2, customerId)
				.setParameter(3, sourceId)
				.setParameter(4, externalId)
				.setParameter(5, merchantId)
				.setParameter(6, categoryId)
				.setParameter(7, new BigDecimal(amount))
				.setParameter(8, direction)
				.setParameter(9, occurredAt)
				.executeUpdate();
	}

	@Test
	void customerTotalsSumsIncomeAndExpenditureExcludingRejectedAndOutOfRangeRows() {
		CustomerTotalsRow totals = adapter.customerTotals(customerId, RANGE_FROM, RANGE_TO_EXCLUSIVE);

		assertThat(totals.totalIncome()).isEqualByComparingTo("5000.00");
		assertThat(totals.totalExpenditure()).isEqualByComparingTo("180.00");
		assertThat(totals.transactionCount()).isEqualTo(4);
	}

	@Test
	void customerTotalsReturnsZeroNotNullForACustomerWithNoTransactions() {
		CustomerTotalsRow totals = adapter.customerTotals(otherCustomerId, RANGE_FROM, RANGE_TO_EXCLUSIVE);

		assertThat(totals.totalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(totals.totalExpenditure()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(totals.transactionCount()).isZero();
	}

	@Test
	void categoryTotalsGroupsDebitTransactionsOnlyByCategory() {
		List<CategoryTotalsRow> totals = adapter.categoryTotals(customerId, RANGE_FROM, RANGE_TO_EXCLUSIVE);

		assertThat(totals).hasSize(2);
		assertThat(totals).anySatisfy(row -> {
			assertThat(row.categoryId()).isEqualTo(groceriesId);
			assertThat(row.totalAmount()).isEqualByComparingTo("130.00");
			assertThat(row.transactionCount()).isEqualTo(2);
		});
		assertThat(totals).anySatisfy(row -> {
			assertThat(row.categoryId()).isEqualTo(fuelId);
			assertThat(row.totalAmount()).isEqualByComparingTo("50.00");
			assertThat(row.transactionCount()).isEqualTo(1);
		});
	}

	@Test
	void categoryTotalsReturnsEmptyListForACustomerWithNoTransactions() {
		assertThat(adapter.categoryTotals(otherCustomerId, RANGE_FROM, RANGE_TO_EXCLUSIVE)).isEmpty();
	}

	@Test
	void merchantTotalsExcludesTransactionsWithNoResolvedMerchant() {
		List<MerchantTotalsRow> totals = adapter.merchantTotals(customerId, RANGE_FROM, RANGE_TO_EXCLUSIVE);

		assertThat(totals).hasSize(2);
		assertThat(totals).anySatisfy(row -> {
			assertThat(row.merchantId()).isEqualTo(checkersId);
			assertThat(row.totalAmount()).isEqualByComparingTo("100.00");
		});
		assertThat(totals).anySatisfy(row -> {
			assertThat(row.merchantId()).isEqualTo(shellId);
			assertThat(row.totalAmount()).isEqualByComparingTo("50.00");
		});
		assertThat(totals).extracting(MerchantTotalsRow::merchantId).doesNotContainNull();
	}

	@Test
	void monthlyTotalsGroupsByMonthAcrossTheRange() {
		List<MonthlyTotalsRow> totals = adapter.monthlyTotals(customerId, RANGE_FROM, RANGE_TO_EXCLUSIVE);

		assertThat(totals).hasSize(2);
		assertThat(totals).anySatisfy(row -> {
			assertThat(row.month()).isEqualTo(YearMonth.of(2026, 1));
			assertThat(row.totalIncome()).isEqualByComparingTo("5000.00");
			assertThat(row.totalExpenditure()).isEqualByComparingTo("100.00");
			assertThat(row.transactionCount()).isEqualTo(2);
		});
		assertThat(totals).anySatisfy(row -> {
			assertThat(row.month()).isEqualTo(YearMonth.of(2026, 2));
			assertThat(row.totalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
			assertThat(row.totalExpenditure()).isEqualByComparingTo("80.00");
			assertThat(row.transactionCount()).isEqualTo(2);
		});
	}

	@Test
	void monthlyTotalsReturnsEmptyListForACustomerWithNoTransactions() {
		assertThat(adapter.monthlyTotals(otherCustomerId, RANGE_FROM, RANGE_TO_EXCLUSIVE)).isEmpty();
	}

}
