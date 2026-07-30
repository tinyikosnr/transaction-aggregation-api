package za.co.tinyiko.transactionaggregation.customer.application;

import za.co.tinyiko.transactionaggregation.customer.domain.CustomerId;

/**
 * Thrown by {@link CustomerLookupPort#getRequired(CustomerId)} when no customer exists with
 * the given id. Standalone for now (not extending a shared base) — see {@code CLAUDE.md} for
 * why: a shared exception base has no genuine cross-module consumer yet.
 */
public class CustomerNotFoundException extends RuntimeException {

	public CustomerNotFoundException(CustomerId customerId) {
		super("Customer not found: " + customerId.value());
	}

}
