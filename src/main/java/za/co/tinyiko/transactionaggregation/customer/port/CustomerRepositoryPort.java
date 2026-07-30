package za.co.tinyiko.transactionaggregation.customer.port;

import java.util.Optional;

import za.co.tinyiko.transactionaggregation.customer.domain.Customer;
import za.co.tinyiko.transactionaggregation.customer.domain.CustomerId;

/**
 * Outbound port the customer application layer uses to persist and read customers (TDS §60).
 * Implemented by the JPA adapter in {@code customer.persistence}; the application layer never
 * depends on Spring Data types directly.
 */
public interface CustomerRepositoryPort {

	Customer save(Customer customer);

	Optional<Customer> findById(CustomerId customerId);

	boolean existsById(CustomerId customerId);

	Optional<Customer> findByExternalReference(String externalReference);

}
