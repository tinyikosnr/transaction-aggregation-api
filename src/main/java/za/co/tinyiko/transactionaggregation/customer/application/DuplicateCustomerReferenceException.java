package za.co.tinyiko.transactionaggregation.customer.application;

/**
 * Thrown by {@link RegisterCustomerUseCase#register} when the requested external reference is
 * already in use (SAD 27.1 — external reference must be unique). Standalone for now, matching
 * {@link CustomerNotFoundException}.
 */
public class DuplicateCustomerReferenceException extends RuntimeException {

	public DuplicateCustomerReferenceException(String externalReference) {
		super("A customer with external reference '" + externalReference + "' already exists");
	}

}
