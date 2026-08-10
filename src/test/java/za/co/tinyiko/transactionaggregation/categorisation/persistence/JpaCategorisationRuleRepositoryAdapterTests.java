package za.co.tinyiko.transactionaggregation.categorisation.persistence;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import za.co.tinyiko.transactionaggregation.TestcontainersConfiguration;
import za.co.tinyiko.transactionaggregation.categorisation.application.RuleConflictException;
import za.co.tinyiko.transactionaggregation.categorisation.application.RuleNotFoundException;
import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRule;
import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRuleId;
import za.co.tinyiko.transactionaggregation.categorisation.domain.Direction;
import za.co.tinyiko.transactionaggregation.categorisation.domain.MatchField;
import za.co.tinyiko.transactionaggregation.categorisation.domain.MatchOperator;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategoryId;
import za.co.tinyiko.transactionaggregation.categorisation.port.ActiveCategorisationRule;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategorisationRuleRow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the port -> adapter -> JPA -> Testcontainers-PostgreSQL chain, including the
 * V4/V6 migrations Flyway applies at context startup. No write path exists for this port
 * either, so this only reads the seeded TDS 37 rule set (all seeded rows are active).
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaCategorisationRuleRepositoryAdapterTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-10T08:15:00Z"), ZoneOffset.UTC);

	@Autowired
	private SpringDataCategorisationRuleRepository springDataCategorisationRuleRepository;

	@Autowired
	private SpringDataTransactionCategoryRepository springDataTransactionCategoryRepository;

	private JpaCategorisationRuleRepositoryAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new JpaCategorisationRuleRepositoryAdapter(springDataCategorisationRuleRepository);
	}

	private TransactionCategoryId aRealCategoryId() {
		return new TransactionCategoryId(springDataTransactionCategoryRepository.findByCode("GROCERIES").orElseThrow().getId());
	}

	private CategorisationRuleRow insertRule() {
		CategorisationRule rule = CategorisationRule.register(CategorisationRuleId.generate(), aRealCategoryId(),
				MatchField.MERCHANT, MatchOperator.CONTAINS, "TESTMERCHANT", Direction.DEBIT, 5, FIXED_CLOCK);
		return adapter.create(rule);
	}

	private static List<CategorisationRule> rulesOf(List<ActiveCategorisationRule> activeRules) {
		return activeRules.stream().map(ActiveCategorisationRule::rule).toList();
	}

	@Test
	void findAllActiveReturnsAllSeededRules() {
		List<ActiveCategorisationRule> activeRules = adapter.findAllActive();

		assertThat(activeRules).hasSize(40);
		assertThat(rulesOf(activeRules)).allMatch(CategorisationRule::active);
	}

	@Test
	void seededRulesIncludeTheFuelMerchantKeyword() {
		List<CategorisationRule> rules = rulesOf(adapter.findAllActive());

		assertThat(rules).anySatisfy(rule -> {
			assertThat(rule.matchField()).isEqualTo(MatchField.MERCHANT);
			assertThat(rule.operator()).isEqualTo(MatchOperator.CONTAINS);
			assertThat(rule.matchValue()).isEqualTo("SHELL");
			assertThat(rule.direction()).isEqualTo(Direction.DEBIT);
			assertThat(rule.priority()).isEqualTo(30);
		});
	}

	@Test
	void seededRulesIncludeBothDirectionWideCatchAlls() {
		List<CategorisationRule> rules = rulesOf(adapter.findAllActive());

		assertThat(rules).anySatisfy(rule -> {
			assertThat(rule.priority()).isEqualTo(999);
			assertThat(rule.direction()).isEqualTo(Direction.CREDIT);
			assertThat(rule.operator()).isEqualTo(MatchOperator.REGEX);
			assertThat(rule.matchValue()).isEqualTo(".*");
		});
		assertThat(rules).anySatisfy(rule -> {
			assertThat(rule.priority()).isEqualTo(1000);
			assertThat(rule.direction()).isEqualTo(Direction.DEBIT);
			assertThat(rule.operator()).isEqualTo(MatchOperator.REGEX);
			assertThat(rule.matchValue()).isEqualTo(".*");
		});
	}

	/**
	 * The join {@code findAllActive()} now performs (feature/observability) must report the
	 * seeded priority-1000 DEBIT catch-all rule's target category (UNCATEGORISED) as the fallback
	 * category, and an ordinary, specific rule's target category (GROCERIES) as not - proving the
	 * enrichment reads the real {@code is_fallback} column, not a hard-coded assumption.
	 */
	@Test
	void reportsCategoryIsFallbackCorrectlyForTheCatchAllRuleAndAnOrdinaryRule() {
		List<ActiveCategorisationRule> activeRules = adapter.findAllActive();

		assertThat(activeRules).anySatisfy(activeRule -> {
			assertThat(activeRule.rule().priority()).isEqualTo(1000);
			assertThat(activeRule.rule().direction()).isEqualTo(Direction.DEBIT);
			assertThat(activeRule.categoryIsFallback()).isTrue();
		});
		assertThat(activeRules).anySatisfy(activeRule -> {
			assertThat(activeRule.rule().matchValue()).isEqualTo("SHELL");
			assertThat(activeRule.categoryIsFallback()).isFalse();
		});
	}

	@Test
	void findAllReturnsEverySeededRuleRegardlessOfActiveStatus() {
		List<CategorisationRuleRow> rows = adapter.findAll();

		assertThat(rows).hasSize(40);
		assertThat(rows).allSatisfy(row -> assertThat(row.version()).isZero());
	}

	@Test
	void createPersistsANewRuleStartingAtVersionZero() {
		CategorisationRuleRow saved = insertRule();

		assertThat(saved.matchValue()).isEqualTo("TESTMERCHANT");
		assertThat(saved.active()).isTrue();
		assertThat(saved.version()).isZero();
		assertThat(adapter.findRowById(new CategorisationRuleId(saved.id()))).isPresent();
	}

	@Test
	void updateAppliesFieldChangesAndIncrementsVersion() {
		CategorisationRuleRow created = insertRule();
		CategorisationRuleId id = new CategorisationRuleId(created.id());
		CategorisationRule updatedFields = CategorisationRule.update(id, aRealCategoryId(),
				MatchField.MERCHANT, MatchOperator.CONTAINS, "TESTMERCHANT", Direction.DEBIT, 500, false, FIXED_CLOCK);

		CategorisationRuleRow updated = adapter.update(id, updatedFields, created.version());

		assertThat(updated.priority()).isEqualTo(500);
		assertThat(updated.active()).isFalse();
		assertThat(updated.version()).isEqualTo(1L);
	}

	@Test
	void updateThrowsRuleNotFoundForAnUnknownId() {
		CategorisationRuleId unknownId = CategorisationRuleId.generate();
		CategorisationRule updatedFields = CategorisationRule.update(unknownId, aRealCategoryId(),
				MatchField.MERCHANT, MatchOperator.CONTAINS, "X", Direction.DEBIT, 5, true, FIXED_CLOCK);

		assertThatThrownBy(() -> adapter.update(unknownId, updatedFields, 0L))
				.isInstanceOf(RuleNotFoundException.class);
	}

	@Test
	void updateThrowsRuleConflictWhenExpectedVersionIsStale() {
		CategorisationRuleRow created = insertRule();
		CategorisationRuleId id = new CategorisationRuleId(created.id());
		CategorisationRule updatedFields = CategorisationRule.update(id, aRealCategoryId(),
				MatchField.MERCHANT, MatchOperator.CONTAINS, "TESTMERCHANT", Direction.DEBIT, 5, true, FIXED_CLOCK);
		long staleVersion = created.version() + 1;

		assertThatThrownBy(() -> adapter.update(id, updatedFields, staleVersion))
				.isInstanceOf(RuleConflictException.class);
	}

	@Test
	void deactivatingARuleRemovesItFromFindAllActive() {
		CategorisationRuleRow created = insertRule();
		CategorisationRuleId id = new CategorisationRuleId(created.id());
		assertThat(rulesOf(adapter.findAllActive())).extracting(CategorisationRule::id).contains(id);

		CategorisationRule deactivated = CategorisationRule.update(id, aRealCategoryId(),
				MatchField.MERCHANT, MatchOperator.CONTAINS, "TESTMERCHANT", Direction.DEBIT, 5, false, FIXED_CLOCK);
		adapter.update(id, deactivated, created.version());

		assertThat(rulesOf(adapter.findAllActive())).extracting(CategorisationRule::id).doesNotContain(id);
	}

}
