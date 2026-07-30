package za.co.tinyiko.transactionaggregation.customer.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import za.co.tinyiko.transactionaggregation.customer.domain.Customer;
import za.co.tinyiko.transactionaggregation.customer.domain.CustomerId;
import za.co.tinyiko.transactionaggregation.customer.mapper.CustomerMapper;
import za.co.tinyiko.transactionaggregation.customer.port.CustomerRepositoryPort;

@Component
class JpaCustomerRepositoryAdapter implements CustomerRepositoryPort {

	private final SpringDataCustomerRepository springDataCustomerRepository;

	JpaCustomerRepositoryAdapter(SpringDataCustomerRepository springDataCustomerRepository) {
		this.springDataCustomerRepository = springDataCustomerRepository;
	}

	@Override
	@Transactional
	public Customer save(Customer customer) {
		CustomerEntity entity = springDataCustomerRepository.findById(customer.id().value())
				.orElseGet(CustomerEntity::new);
		CustomerMapper.applyTo(entity, customer);
		CustomerEntity saved = springDataCustomerRepository.save(entity);
		return CustomerMapper.toDomain(saved);
	}

	@Override
	public Optional<Customer> findById(CustomerId customerId) {
		return springDataCustomerRepository.findById(customerId.value())
				.map(CustomerMapper::toDomain);
	}

	@Override
	public boolean existsById(CustomerId customerId) {
		return springDataCustomerRepository.existsById(customerId.value());
	}

	@Override
	public Optional<Customer> findByExternalReference(String externalReference) {
		return springDataCustomerRepository.findByExternalReference(externalReference)
				.map(CustomerMapper::toDomain);
	}

}
