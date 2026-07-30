package za.co.tinyiko.transactionaggregation.customer.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.tinyiko.transactionaggregation.customer.domain.Customer;
import za.co.tinyiko.transactionaggregation.customer.domain.CustomerId;
import za.co.tinyiko.transactionaggregation.customer.port.CustomerRepositoryPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerLookupServiceTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-30T08:15:00Z"), ZoneOffset.UTC);

	@Mock
	private CustomerRepositoryPort customerRepositoryPort;

	@Test
	void getRequiredReturnsCustomerWhenPresent() {
		CustomerId id = CustomerId.generate();
		Customer customer = Customer.register(id, "EXT-001", "Jane", "Doe", null, FIXED_CLOCK);
		when(customerRepositoryPort.findById(id)).thenReturn(Optional.of(customer));

		CustomerLookupService service = new CustomerLookupService(customerRepositoryPort);

		assertThat(service.getRequired(id)).isEqualTo(customer);
	}

	@Test
	void getRequiredThrowsWhenAbsent() {
		CustomerId id = CustomerId.generate();
		when(customerRepositoryPort.findById(id)).thenReturn(Optional.empty());

		CustomerLookupService service = new CustomerLookupService(customerRepositoryPort);

		assertThatThrownBy(() -> service.getRequired(id)).isInstanceOf(CustomerNotFoundException.class);
	}

	@Test
	void existsDelegatesToRepository() {
		CustomerId id = CustomerId.generate();
		when(customerRepositoryPort.existsById(id)).thenReturn(true);

		CustomerLookupService service = new CustomerLookupService(customerRepositoryPort);

		assertThat(service.exists(id)).isTrue();
		verify(customerRepositoryPort).existsById(id);
	}

}
