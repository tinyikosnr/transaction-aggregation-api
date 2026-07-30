package za.co.tinyiko.transactionaggregation.customer.persistence;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import za.co.tinyiko.transactionaggregation.TestcontainersConfiguration;
import za.co.tinyiko.transactionaggregation.customer.domain.Customer;
import za.co.tinyiko.transactionaggregation.customer.domain.CustomerId;
import za.co.tinyiko.transactionaggregation.customer.domain.CustomerStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the full port → adapter → JPA → Testcontainers-PostgreSQL chain, including the
 * {@code V1__create_customers_table.sql} migration Flyway applies at context startup.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaCustomerRepositoryAdapterTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-30T08:15:00Z"), ZoneOffset.UTC);

	@Autowired
	private SpringDataCustomerRepository springDataCustomerRepository;

	private JpaCustomerRepositoryAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new JpaCustomerRepositoryAdapter(springDataCustomerRepository);
	}

	@Test
	void savePersistsAndFindByIdReturnsTheCustomer() {
		Customer customer = Customer.register(CustomerId.generate(), "EXT-001", "Jane", "Doe",
				"jane@example.com", FIXED_CLOCK);

		Customer saved = adapter.save(customer);

		assertThat(saved.externalReference()).isEqualTo("EXT-001");
		assertThat(adapter.findById(customer.id())).contains(saved);
	}

	@Test
	void saveUpdatesAnExistingCustomer() {
		Customer customer = Customer.register(CustomerId.generate(), "EXT-002", "Jane", "Doe", null, FIXED_CLOCK);
		adapter.save(customer);

		customer.suspend(FIXED_CLOCK);
		adapter.save(customer);

		Customer reloaded = adapter.findById(customer.id()).orElseThrow();
		assertThat(reloaded.status()).isEqualTo(CustomerStatus.SUSPENDED);
	}

	@Test
	void findByIdReturnsEmptyWhenAbsent() {
		assertThat(adapter.findById(CustomerId.generate())).isEmpty();
	}

	@Test
	void existsByIdReflectsPersistedState() {
		Customer customer = Customer.register(CustomerId.generate(), "EXT-003", "Jane", "Doe", null, FIXED_CLOCK);

		assertThat(adapter.existsById(customer.id())).isFalse();

		adapter.save(customer);

		assertThat(adapter.existsById(customer.id())).isTrue();
	}

	@Test
	void findByExternalReferenceReturnsMatchingCustomer() {
		Customer customer = Customer.register(CustomerId.generate(), "EXT-004", "Jane", "Doe", null, FIXED_CLOCK);
		adapter.save(customer);

		assertThat(adapter.findByExternalReference("EXT-004")).contains(customer);
		assertThat(adapter.findByExternalReference("NO-SUCH-REFERENCE")).isEmpty();
	}

	@Test
	void duplicateExternalReferenceIsRejectedByTheDatabase() {
		adapter.save(Customer.register(CustomerId.generate(), "EXT-005", "Jane", "Doe", null, FIXED_CLOCK));
		Customer duplicate = Customer.register(CustomerId.generate(), "EXT-005", "John", "Smith", null, FIXED_CLOCK);

		assertThatThrownBy(() -> {
			adapter.save(duplicate);
			springDataCustomerRepository.flush();
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

}
