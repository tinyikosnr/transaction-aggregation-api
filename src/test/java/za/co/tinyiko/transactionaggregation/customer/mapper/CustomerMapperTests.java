package za.co.tinyiko.transactionaggregation.customer.mapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.customer.domain.Customer;
import za.co.tinyiko.transactionaggregation.customer.domain.CustomerId;
import za.co.tinyiko.transactionaggregation.customer.domain.CustomerStatus;
import za.co.tinyiko.transactionaggregation.customer.persistence.CustomerEntity;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerMapperTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-30T08:15:00Z"), ZoneOffset.UTC);

	@Test
	void toDomainMapsAllFields() {
		CustomerEntity entity = new CustomerEntity();
		UUID id = UUID.randomUUID();
		entity.setId(id);
		entity.setExternalReference("EXT-001");
		entity.setFirstName("Jane");
		entity.setLastName("Doe");
		entity.setEmailAddress("jane@example.com");
		entity.setStatus(CustomerStatus.ACTIVE);
		entity.setCreatedAt(Instant.parse("2026-07-30T08:15:00Z"));
		entity.setUpdatedAt(Instant.parse("2026-07-30T09:00:00Z"));

		Customer customer = CustomerMapper.toDomain(entity);

		assertThat(customer.id()).isEqualTo(new CustomerId(id));
		assertThat(customer.externalReference()).isEqualTo("EXT-001");
		assertThat(customer.firstName()).isEqualTo("Jane");
		assertThat(customer.lastName()).isEqualTo("Doe");
		assertThat(customer.emailAddress()).isEqualTo("jane@example.com");
		assertThat(customer.status()).isEqualTo(CustomerStatus.ACTIVE);
		assertThat(customer.createdAt()).isEqualTo(Instant.parse("2026-07-30T08:15:00Z"));
		assertThat(customer.updatedAt()).isEqualTo(Instant.parse("2026-07-30T09:00:00Z"));
	}

	@Test
	void toDomainHandlesNullEmail() {
		CustomerEntity entity = new CustomerEntity();
		entity.setId(UUID.randomUUID());
		entity.setExternalReference("EXT-002");
		entity.setFirstName("Jane");
		entity.setLastName("Doe");
		entity.setEmailAddress(null);
		entity.setStatus(CustomerStatus.ACTIVE);
		entity.setCreatedAt(Instant.parse("2026-07-30T08:15:00Z"));
		entity.setUpdatedAt(Instant.parse("2026-07-30T08:15:00Z"));

		Customer customer = CustomerMapper.toDomain(entity);

		assertThat(customer.emailAddress()).isNull();
	}

	@Test
	void applyToCopiesAllFieldsOntoEntity() {
		Customer customer = Customer.register(CustomerId.generate(), "EXT-003", "John", "Smith",
				"john@example.com", FIXED_CLOCK);
		CustomerEntity entity = new CustomerEntity();

		CustomerMapper.applyTo(entity, customer);

		assertThat(entity.getId()).isEqualTo(customer.id().value());
		assertThat(entity.getExternalReference()).isEqualTo("EXT-003");
		assertThat(entity.getFirstName()).isEqualTo("John");
		assertThat(entity.getLastName()).isEqualTo("Smith");
		assertThat(entity.getEmailAddress()).isEqualTo("john@example.com");
		assertThat(entity.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
		assertThat(entity.getCreatedAt()).isEqualTo(customer.createdAt());
		assertThat(entity.getUpdatedAt()).isEqualTo(customer.updatedAt());
	}

	@Test
	void applyToDoesNotTouchVersion() {
		Customer customer = Customer.register(CustomerId.generate(), "EXT-004", "John", "Smith", null, FIXED_CLOCK);
		CustomerEntity entity = new CustomerEntity();

		CustomerMapper.applyTo(entity, customer);

		assertThat(entity.getVersion()).isNull();
	}

}
