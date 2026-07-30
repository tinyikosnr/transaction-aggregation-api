package za.co.tinyiko.transactionaggregation.customer.application;

import za.co.tinyiko.transactionaggregation.customer.domain.CustomerId;

/**
 * Inbound port other modules use to check whether a customer exists, without needing the full
 * aggregate (TDS §7).
 */
public interface CustomerExistsPort {

	boolean exists(CustomerId customerId);

}
