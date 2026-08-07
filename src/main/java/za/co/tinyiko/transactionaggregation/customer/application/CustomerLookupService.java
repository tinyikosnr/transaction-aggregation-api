package za.co.tinyiko.transactionaggregation.customer.application;

import java.util.UUID;

import org.springframework.stereotype.Service;

import za.co.tinyiko.transactionaggregation.customer.domain.Customer;
import za.co.tinyiko.transactionaggregation.customer.domain.CustomerId;
import za.co.tinyiko.transactionaggregation.customer.port.CustomerRepositoryPort;

@Service
class CustomerLookupService implements CustomerLookupPort, CustomerExistsPort {

	private final CustomerRepositoryPort customerRepositoryPort;

	CustomerLookupService(CustomerRepositoryPort customerRepositoryPort) {
		this.customerRepositoryPort = customerRepositoryPort;
	}

	@Override
	public Customer getRequired(CustomerId customerId) {
		return customerRepositoryPort.findById(customerId)
				.orElseThrow(() -> new CustomerNotFoundException(customerId));
	}

	@Override
	public boolean exists(UUID customerId) {
		return customerRepositoryPort.existsById(new CustomerId(customerId));
	}

}
