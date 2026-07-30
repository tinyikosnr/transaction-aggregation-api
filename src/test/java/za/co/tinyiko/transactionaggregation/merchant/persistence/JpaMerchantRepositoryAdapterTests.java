package za.co.tinyiko.transactionaggregation.merchant.persistence;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import za.co.tinyiko.transactionaggregation.TestcontainersConfiguration;
import za.co.tinyiko.transactionaggregation.merchant.application.DuplicateMerchantException;
import za.co.tinyiko.transactionaggregation.merchant.domain.Merchant;
import za.co.tinyiko.transactionaggregation.merchant.domain.MerchantId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the full port → adapter → JPA → Testcontainers-PostgreSQL chain, including the
 * {@code V2__create_merchants_table.sql} migration Flyway applies at context startup.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaMerchantRepositoryAdapterTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-31T08:15:00Z"), ZoneOffset.UTC);

	@Autowired
	private SpringDataMerchantRepository springDataMerchantRepository;

	private JpaMerchantRepositoryAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new JpaMerchantRepositoryAdapter(springDataMerchantRepository);
	}

	@Test
	void savePersistsAndFindByNormalisedNameReturnsTheMerchant() {
		Merchant merchant = Merchant.register(MerchantId.generate(), "CHECKERS 104 CENTURION",
				"Checkers #104 Centurion", FIXED_CLOCK);

		Merchant saved = adapter.save(merchant);

		assertThat(saved.normalisedName()).isEqualTo("CHECKERS 104 CENTURION");
		assertThat(adapter.findByNormalisedName("CHECKERS 104 CENTURION")).contains(saved);
	}

	@Test
	void findByNormalisedNameReturnsEmptyWhenAbsent() {
		assertThat(adapter.findByNormalisedName("NO-SUCH-MERCHANT")).isEmpty();
	}

	@Test
	void duplicateNormalisedNameIsTranslatedIntoADomainException() {
		adapter.save(Merchant.register(MerchantId.generate(), "SPAR", "Spar", FIXED_CLOCK));
		Merchant duplicate = Merchant.register(MerchantId.generate(), "SPAR", "SPAR Centurion", FIXED_CLOCK);

		assertThatThrownBy(() -> adapter.save(duplicate))
				.isInstanceOf(DuplicateMerchantException.class)
				.hasMessageContaining("SPAR");
	}

}
