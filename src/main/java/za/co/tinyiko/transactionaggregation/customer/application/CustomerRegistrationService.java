package za.co.tinyiko.transactionaggregation.customer.application;

import java.time.Clock;

import org.springframework.stereotype.Service;

import za.co.tinyiko.transactionaggregation.customer.domain.Customer;
import za.co.tinyiko.transactionaggregation.customer.domain.CustomerId;
import za.co.tinyiko.transactionaggregation.customer.port.CustomerRepositoryPort;

@Service
class CustomerRegistrationService implements RegisterCustomerUseCase {

	private final CustomerRepositoryPort customerRepositoryPort;
	private final Clock clock;

	CustomerRegistrationService(CustomerRepositoryPort customerRepositoryPort, Clock clock) {
		this.customerRepositoryPort = customerRepositoryPort;
		this.clock = clock;
	}

	@Override
	public Customer register(RegisterCustomerCommand command) {
		if (customerRepositoryPort.findByExternalReference(command.externalReference()).isPresent()) {
			throw new DuplicateCustomerReferenceException(command.externalReference());
		}

		Customer customer = Customer.register(
				CustomerId.generate(),
				command.externalReference(),
				command.firstName(),
				command.lastName(),
				command.emailAddress(),
				clock);

		return customerRepositoryPort.save(customer);
	}

}
