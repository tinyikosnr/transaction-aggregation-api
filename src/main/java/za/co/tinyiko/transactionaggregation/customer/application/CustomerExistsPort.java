package za.co.tinyiko.transactionaggregation.customer.application;

import java.util.UUID;

/**
 * Inbound port other modules use to check whether a customer exists, without needing the full
 * aggregate (TDS 7).
 *
 * <p>Takes a raw {@code UUID}, not {@code customer.domain.CustomerId}: this package is exposed
 * cross-module via {@code @NamedInterface}, but {@code customer.domain} is not, and never needs
 * to be - a boolean existence check has no reason to force a caller to reach into another
 * module's domain package just to construct an id wrapper.
 */
public interface CustomerExistsPort {

	boolean exists(UUID customerId);

}
