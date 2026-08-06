package za.co.tinyiko.transactionaggregation.categorisation.persistence;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import za.co.tinyiko.transactionaggregation.TestcontainersConfiguration;
import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRule;
import za.co.tinyiko.transactionaggregation.categorisation.domain.Direction;
import za.co.tinyiko.transactionaggregation.categorisation.domain.MatchField;
import za.co.tinyiko.transactionaggregation.categorisation.domain.MatchOperator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the port -> adapter -> JPA -> Testcontainers-PostgreSQL chain, including the
 * V4/V6 migrations Flyway applies at context startup. No write path exists for this port
 * either, so this only reads the seeded TDS 37 rule set (all seeded rows are active).
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaCategorisationRuleRepositoryAdapterTests {

	@Autowired
	private SpringDataCategorisationRuleRepository springDataCategorisationRuleRepository;

	private JpaCategorisationRuleRepositoryAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new JpaCategorisationRuleRepositoryAdapter(springDataCategorisationRuleRepository);
	}

	@Test
	void findAllActiveReturnsAllSeededRules() {
		List<CategorisationRule> rules = adapter.findAllActive();

		assertThat(rules).hasSize(40);
		assertThat(rules).allMatch(CategorisationRule::active);
	}

	@Test
	void seededRulesIncludeTheFuelMerchantKeyword() {
		List<CategorisationRule> rules = adapter.findAllActive();

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
		List<CategorisationRule> rules = adapter.findAllActive();

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

}
