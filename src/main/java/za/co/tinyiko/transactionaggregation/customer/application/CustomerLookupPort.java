package za.co.tinyiko.transactionaggregation.customer.application;

import za.co.tinyiko.transactionaggregation.customer.domain.Customer;
import za.co.tinyiko.transactionaggregation.customer.domain.CustomerId;

/**
 * Inbound port other modules use to look up a customer that is required to exist (TDS §7).
 */
public interface CustomerLookupPort {

	/**
	 * @throws CustomerNotFoundException if no customer exists with the given id
	 */
	Customer getRequired(CustomerId customerId);

}
