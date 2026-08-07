package za.co.tinyiko.transactionaggregation.transaction.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import za.co.tinyiko.transactionaggregation.TestcontainersConfiguration;
import za.co.tinyiko.transactionaggregation.transaction.domain.SourceStatus;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the port -> adapter -> JPA -> Testcontainers-PostgreSQL chain, including the
 * V8/V10 migrations Flyway applies at context startup. No write path exists for this port
 * (see {@link TransactionSourceEntity}'s Javadoc), so this only reads the seeded TDS 11
 * initial sources.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaTransactionSourceRepositoryAdapterTests {

	@Autowired
	private SpringDataTransactionSourceRepository springDataTransactionSourceRepository;

	private JpaTransactionSourceRepositoryAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new JpaTransactionSourceRepositoryAdapter(springDataTransactionSourceRepository);
	}

	@Test
	void findByCodeReturnsASeededActiveSource() {
		var source = adapter.findByCode("MOCK_BANK_A");

		assertThat(source).isPresent();
		TransactionSource resolved = source.get();
		assertThat(resolved.code()).isEqualTo("MOCK_BANK_A");
		assertThat(resolved.status()).isEqualTo(SourceStatus.ACTIVE);
		assertThat(resolved.isActive()).isTrue();
	}

	@Test
	void findByCodeReturnsEmptyWhenAbsent() {
		assertThat(adapter.findByCode("NO-SUCH-SOURCE")).isEmpty();
	}

	@Test
	void allThreeInitialSourcesAreSeededAndActive() {
		assertThat(adapter.findByCode("MOCK_BANK_A")).isPresent().get().extracting(TransactionSource::isActive).isEqualTo(true);
		assertThat(adapter.findByCode("MOCK_BANK_B")).isPresent().get().extracting(TransactionSource::isActive).isEqualTo(true);
		assertThat(adapter.findByCode("MANUAL_UPLOAD")).isPresent().get().extracting(TransactionSource::isActive).isEqualTo(true);
	}

}
