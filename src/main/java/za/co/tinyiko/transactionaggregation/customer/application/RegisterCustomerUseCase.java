package za.co.tinyiko.transactionaggregation.customer.application;

import za.co.tinyiko.transactionaggregation.customer.domain.Customer;

/**
 * Inbound port for registering a new customer. Not named in the TDS's class catalogue, but a
 * necessary capability — {@code customers} is the only source of rows for the foreign key
 * every transaction requires (BR-01), and no other module owns this table.
 */
public interface RegisterCustomerUseCase {

	/**
	 * @throws DuplicateCustomerReferenceException if a customer already exists with the given
	 * external reference
	 */
	Customer register(RegisterCustomerCommand command);

}
