package za.co.tinyiko.transactionaggregation.merchant.application;

/**
 * Thrown by the persistence adapter when a save attempt violates the {@code merchants}
 * table's unique constraint on {@code normalised_name} — translating the underlying
 * persistence exception into a domain/application type rather than leaking it. Normally
 * caught and recovered from within {@link MerchantResolutionService} (a concurrent request
 * created the same merchant first); only escapes to a caller if that recovery itself finds
 * nothing, which should not happen in practice.
 */
public class DuplicateMerchantException extends RuntimeException {

	public DuplicateMerchantException(String normalisedName, Throwable cause) {
		super("A merchant with normalised name '" + normalisedName + "' already exists", cause);
	}

}
