package za.co.tinyiko.transactionaggregation.customer.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class CustomerTests {

	private static final Instant FIXED_INSTANT = Instant.parse("2026-07-30T08:15:00Z");
	private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

	private static CustomerId anId() {
		return CustomerId.generate();
	}

	@Test
	void registerCreatesActiveCustomerWithTimestamps() {
		CustomerId id = anId();

		Customer customer = Customer.register(id, "EXT-001", "Jane", "Doe", "jane.doe@example.com", FIXED_CLOCK);

		assertThat(customer.id()).isEqualTo(id);
		assertThat(customer.externalReference()).isEqualTo("EXT-001");
		assertThat(customer.firstName()).isEqualTo("Jane");
		assertThat(customer.lastName()).isEqualTo("Doe");
		assertThat(customer.emailAddress()).isEqualTo("jane.doe@example.com");
		assertThat(customer.status()).isEqualTo(CustomerStatus.ACTIVE);
		assertThat(customer.createdAt()).isEqualTo(FIXED_INSTANT);
		assertThat(customer.updatedAt()).isEqualTo(FIXED_INSTANT);
	}

	@Test
	void registerAllowsNullEmail() {
		Customer customer = Customer.register(anId(), "EXT-002", "Jane", "Doe", null, FIXED_CLOCK);

		assertThat(customer.emailAddress()).isNull();
	}

	@Test
	void registerRejectsNullId() {
		assertThatNullPointerException()
				.isThrownBy(() -> Customer.register(null, "EXT-003", "Jane", "Doe", null, FIXED_CLOCK));
	}

	@Test
	void registerRejectsBlankExternalReference() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Customer.register(anId(), "  ", "Jane", "Doe", null, FIXED_CLOCK));
	}

	@Test
	void registerRejectsExternalReferenceTooLong() {
		String tooLong = "x".repeat(101);

		assertThatIllegalArgumentException()
				.isThrownBy(() -> Customer.register(anId(), tooLong, "Jane", "Doe", null, FIXED_CLOCK));
	}

	@Test
	void registerRejectsBlankFirstName() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Customer.register(anId(), "EXT-004", " ", "Doe", null, FIXED_CLOCK));
	}

	@Test
	void registerRejectsBlankLastName() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Customer.register(anId(), "EXT-005", "Jane", " ", null, FIXED_CLOCK));
	}

	@Test
	void registerRejectsInvalidEmailFormat() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Customer.register(anId(), "EXT-006", "Jane", "Doe", "not-an-email", FIXED_CLOCK));
	}

	@Test
	void registerRejectsEmailTooLong() {
		String tooLong = "a".repeat(250) + "@x.com";

		assertThatIllegalArgumentException()
				.isThrownBy(() -> Customer.register(anId(), "EXT-007", "Jane", "Doe", tooLong, FIXED_CLOCK));
	}

	@Test
	void activateSetsStatusAndBumpsUpdatedAt() {
		Customer customer = Customer.register(anId(), "EXT-008", "Jane", "Doe", null, FIXED_CLOCK);
		Instant later = FIXED_INSTANT.plusSeconds(60);
		Clock laterClock = Clock.fixed(later, ZoneOffset.UTC);

		customer.suspend(laterClock);
		assertThat(customer.status()).isEqualTo(CustomerStatus.SUSPENDED);
		assertThat(customer.updatedAt()).isEqualTo(later);

		customer.activate(laterClock);
		assertThat(customer.status()).isEqualTo(CustomerStatus.ACTIVE);
	}

	@Test
	void deactivateSetsStatus() {
		Customer customer = Customer.register(anId(), "EXT-009", "Jane", "Doe", null, FIXED_CLOCK);

		customer.deactivate(FIXED_CLOCK);

		assertThat(customer.status()).isEqualTo(CustomerStatus.INACTIVE);
	}

	@Test
	void equalityIsByIdOnly() {
		CustomerId id = anId();
		Customer first = Customer.register(id, "EXT-010", "Jane", "Doe", null, FIXED_CLOCK);
		Customer second = Customer.reconstitute(id, "EXT-010-DIFFERENT", "John", "Smith", "john@example.com",
				CustomerStatus.SUSPENDED, FIXED_INSTANT, FIXED_INSTANT);
		Customer differentId = Customer.register(anId(), "EXT-010", "Jane", "Doe", null, FIXED_CLOCK);

		assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
		assertThat(first).isNotEqualTo(differentId);
	}

	@Test
	void reconstituteRebuildsWithoutRunningRegistrationInvariants() {
		CustomerId id = anId();

		Customer customer = Customer.reconstitute(id, "EXT-011", "Jane", "Doe", null,
				CustomerStatus.INACTIVE, FIXED_INSTANT, FIXED_INSTANT);

		assertThat(customer.status()).isEqualTo(CustomerStatus.INACTIVE);
	}

}
