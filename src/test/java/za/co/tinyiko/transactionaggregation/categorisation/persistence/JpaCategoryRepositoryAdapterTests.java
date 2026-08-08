package za.co.tinyiko.transactionaggregation.categorisation.persistence;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import za.co.tinyiko.transactionaggregation.TestcontainersConfiguration;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategory;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategoryId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the port -> adapter -> JPA -> Testcontainers-PostgreSQL chain, including the
 * V3/V5 migrations Flyway applies at context startup. There is no write path for this port
 * (see {@link TransactionCategoryEntity}'s Javadoc), so this only reads the seeded data - the
 * seeded {@code UNCATEGORISED} row is the sole {@code is_fallback = TRUE} category (SAD 27.5,
 * enforced by V3's partial unique index).
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaCategoryRepositoryAdapterTests {

	@Autowired
	private SpringDataTransactionCategoryRepository springDataTransactionCategoryRepository;

	private JpaCategoryRepositoryAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new JpaCategoryRepositoryAdapter(springDataTransactionCategoryRepository);
	}

	@Test
	void findFallbackReturnsTheSeededUncategorisedCategory() {
		Optional<TransactionCategory> fallback = adapter.findFallback();

		assertThat(fallback).isPresent();
		assertThat(fallback.get().code()).isEqualTo("UNCATEGORISED");
		assertThat(fallback.get().fallback()).isTrue();
	}

	@Test
	void onlyOneSeededCategoryIsMarkedAsFallback() {
		long fallbackCount = springDataTransactionCategoryRepository.findAll().stream()
				.filter(TransactionCategoryEntity::isFallback)
				.count();

		assertThat(fallbackCount).isEqualTo(1);
	}

	@Test
	void findByIdReturnsTheMatchingSeededCategory() {
		TransactionCategory fallback = adapter.findFallback().orElseThrow();

		Optional<TransactionCategory> found = adapter.findById(fallback.id());

		assertThat(found).isPresent();
		assertThat(found.get().code()).isEqualTo(fallback.code());
	}

	@Test
	void findByIdReturnsEmptyForAnUnknownId() {
		Optional<TransactionCategory> found = adapter.findById(new TransactionCategoryId(UUID.randomUUID()));

		assertThat(found).isEmpty();
	}

}
