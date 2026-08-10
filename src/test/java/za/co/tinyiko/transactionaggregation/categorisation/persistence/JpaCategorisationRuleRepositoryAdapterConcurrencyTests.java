package za.co.tinyiko.transactionaggregation.categorisation.persistence;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import za.co.tinyiko.transactionaggregation.TestcontainersConfiguration;
import za.co.tinyiko.transactionaggregation.categorisation.application.RuleConflictException;
import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRule;
import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRuleId;
import za.co.tinyiko.transactionaggregation.categorisation.domain.Direction;
import za.co.tinyiko.transactionaggregation.categorisation.domain.MatchField;
import za.co.tinyiko.transactionaggregation.categorisation.domain.MatchOperator;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategoryId;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategorisationRuleRow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the optimistic-lock guarantee against two genuinely independent, separately-committing
 * transactions - not two calls sharing one enclosing test transaction, which {@code @DataJpaTest}
 * would otherwise wrap every test method in by default (and roll back at the end, defeating the
 * point of this test entirely).
 *
 * <p>{@code @Transactional(propagation = Propagation.NOT_SUPPORTED)} on the class suspends that
 * default wrapping, so every call below to {@code adapter.create}/{@code adapter.update} runs
 * with no ambient transaction: each individual Spring Data JPA repository call inside those
 * adapter methods opens and commits its own transaction. This means the entity {@code
 * JpaCategorisationRuleRepositoryAdapter#update} loads becomes <em>detached</em> the moment its
 * own {@code findById} call's transaction closes, and {@code saveAndFlush} on a detached entity
 * with an existing id performs a JPA <em>merge</em> - which enforces the identical
 * version-vs-current-row optimistic check at merge time as the attached-entity case does at
 * flush time (see the adapter's own Javadoc). This test exercises exactly that path, proving the
 * guarantee holds even under two separately-committing calls, not merely within one shared
 * persistence context.
 *
 * <p><strong>Cleans up explicitly in {@code @AfterEach}.</strong> Disabling the ambient
 * transaction also disables {@code @DataJpaTest}'s usual automatic per-test rollback - anything
 * inserted here genuinely commits and would otherwise silently leak into every other test class
 * sharing the same Testcontainers Postgres instance (this was caught empirically: a first version
 * of this test without cleanup made {@code JpaCategorisationRuleRepositoryAdapterTests}' seeded-row
 * count assertions intermittently see 41 rows instead of the expected 40).
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JpaCategorisationRuleRepositoryAdapterConcurrencyTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-10T08:15:00Z"), ZoneOffset.UTC);

	@Autowired
	private SpringDataCategorisationRuleRepository springDataCategorisationRuleRepository;

	@Autowired
	private SpringDataTransactionCategoryRepository springDataTransactionCategoryRepository;

	private JpaCategorisationRuleRepositoryAdapter adapter;
	private CategorisationRuleId insertedRuleId;

	@BeforeEach
	void setUp() {
		adapter = new JpaCategorisationRuleRepositoryAdapter(springDataCategorisationRuleRepository);
	}

	@AfterEach
	void cleanUp() {
		if (insertedRuleId != null) {
			springDataCategorisationRuleRepository.deleteById(insertedRuleId.value());
		}
	}

	@Test
	void secondConcurrentUpdateWithAStaleExpectedVersionIsRejectedAndOnlyTheFirstUpdateIsPersisted() {
		TransactionCategoryId categoryId = new TransactionCategoryId(
				springDataTransactionCategoryRepository.findByCode("GROCERIES").orElseThrow().getId());
		CategorisationRule initial = CategorisationRule.register(CategorisationRuleId.generate(), categoryId,
				MatchField.MERCHANT, MatchOperator.CONTAINS, "RACEMERCHANT", Direction.DEBIT, 5, FIXED_CLOCK);
		CategorisationRuleRow created = adapter.create(initial); // its own, independently-committing insert
		CategorisationRuleId ruleId = new CategorisationRuleId(created.id());
		insertedRuleId = ruleId;

		// 1. Both "transaction A" and "transaction B" read the same starting version, N.
		CategorisationRuleRow readByA = adapter.findRowById(ruleId).orElseThrow();
		CategorisationRuleRow readByB = adapter.findRowById(ruleId).orElseThrow();
		assertThat(readByA.version()).isEqualTo(readByB.version());
		long versionBothObserved = readByA.version();

		// 2. Transaction A updates successfully, its own independently-committing write.
		CategorisationRule updateByA = CategorisationRule.update(ruleId, categoryId,
				MatchField.MERCHANT, MatchOperator.CONTAINS, "RACEMERCHANT-A", Direction.DEBIT, 5, true, FIXED_CLOCK);
		CategorisationRuleRow afterA = adapter.update(ruleId, updateByA, versionBothObserved);
		assertThat(afterA.version()).isEqualTo(versionBothObserved + 1);

		// 3. Transaction B attempts its own, separately-committing update using the SAME
		// originally-observed version N - now stale, since A already advanced it to N+1.
		CategorisationRule updateByB = CategorisationRule.update(ruleId, categoryId,
				MatchField.MERCHANT, MatchOperator.CONTAINS, "RACEMERCHANT-B", Direction.DEBIT, 5, true, FIXED_CLOCK);

		// 4. B must be rejected with the optimistic-lock conflict.
		assertThatThrownBy(() -> adapter.update(ruleId, updateByB, versionBothObserved))
				.isInstanceOf(RuleConflictException.class);

		// 5. Persisted state reflects only A's update - B's attempted change never landed.
		CategorisationRuleRow finalState = adapter.findRowById(ruleId).orElseThrow();
		assertThat(finalState.matchValue()).isEqualTo("RACEMERCHANT-A");
		assertThat(finalState.version()).isEqualTo(versionBothObserved + 1);
	}

}
