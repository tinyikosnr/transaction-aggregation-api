package za.co.tinyiko.transactionaggregation.customer.application;

/**
 * The data required to register a new customer. {@code emailAddress} is optional; every other
 * field is required — see {@link za.co.tinyiko.transactionaggregation.customer.domain.Customer#register}
 * for the invariants actually enforced.
 */
public record RegisterCustomerCommand(
		String externalReference,
		String firstName,
		String lastName,
		String emailAddress
) {
}
