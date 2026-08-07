package za.co.tinyiko.transactionaggregation.transaction.application;

import java.util.UUID;

/**
 * Thrown by {@link CreateTransactionService} when {@code CustomerExistsPort.exists} returns
 * {@code false} for the requested customer id (CUS-001). Deliberately this module's own type,
 * not {@code customer.application.CustomerNotFoundException}: each module signals its own
 * failure scenarios with its own exception types, even where another module happens to already
 * have a same-shaped one - see CLAUDE.md's "business exception" rule.
 */
public class CustomerNotFoundException extends RuntimeException {

	public CustomerNotFoundException(UUID customerId) {
		super("Customer not found: " + customerId);
	}

}
