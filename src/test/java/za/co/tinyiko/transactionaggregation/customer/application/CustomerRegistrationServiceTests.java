package za.co.tinyiko.transactionaggregation.customer.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.tinyiko.transactionaggregation.customer.domain.Customer;
import za.co.tinyiko.transactionaggregation.customer.domain.CustomerId;
import za.co.tinyiko.transactionaggregation.customer.port.CustomerRepositoryPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerRegistrationServiceTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-30T08:15:00Z"), ZoneOffset.UTC);

	@Mock
	private CustomerRepositoryPort customerRepositoryPort;

	@Test
	void registersNewCustomerWhenReferenceIsUnique() {
		when(customerRepositoryPort.findByExternalReference("EXT-001")).thenReturn(Optional.empty());
		when(customerRepositoryPort.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CustomerRegistrationService service = new CustomerRegistrationService(customerRepositoryPort, FIXED_CLOCK);
		RegisterCustomerCommand command = new RegisterCustomerCommand("EXT-001", "Jane", "Doe", "jane@example.com");

		Customer registered = service.register(command);

		assertThat(registered.externalReference()).isEqualTo("EXT-001");
		assertThat(registered.firstName()).isEqualTo("Jane");

		ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
		verify(customerRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().externalReference()).isEqualTo("EXT-001");
	}

	@Test
	void throwsWhenReferenceAlreadyExists() {
		CustomerId existingId = CustomerId.generate();
		Customer existing = Customer.register(existingId, "EXT-002", "John", "Smith", null, FIXED_CLOCK);
		when(customerRepositoryPort.findByExternalReference("EXT-002")).thenReturn(Optional.of(existing));

		CustomerRegistrationService service = new CustomerRegistrationService(customerRepositoryPort, FIXED_CLOCK);
		RegisterCustomerCommand command = new RegisterCustomerCommand("EXT-002", "Jane", "Doe", null);

		assertThatThrownBy(() -> service.register(command))
				.isInstanceOf(DuplicateCustomerReferenceException.class);

		verify(customerRepositoryPort, never()).save(any());
	}

}
