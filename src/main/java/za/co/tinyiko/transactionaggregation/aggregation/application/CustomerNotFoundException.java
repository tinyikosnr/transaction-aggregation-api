package za.co.tinyiko.transactionaggregation.aggregation.application;

import java.util.UUID;

/**
 * Thrown by each {@code Get*SummaryService} when {@code CustomerExistsPort.exists} returns
 * {@code false} for the requested customer id (TDS 40's {@code CUS-001}, reused conceptually).
 * Deliberately this module's own type, not {@code customer.application}'s or
 * {@code transaction.application}'s - each module signals its own failure scenarios with its
 * own exception types.
 */
public class CustomerNotFoundException extends RuntimeException {

	public CustomerNotFoundException(UUID customerId) {
		super("Customer not found: " + customerId);
	}

}
