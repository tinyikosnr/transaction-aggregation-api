package za.co.tinyiko.transactionaggregation.merchant.application;

/**
 * Inbound port other modules use to resolve a raw merchant name into a normalised merchant,
 * matching an existing one or creating a new one as needed (TDS 3, 60).
 *
 * <p>A single, fused capability - deliberately not split into separate lookup/exists/register
 * ports the way {@code customer} was. Customer's three-port split reflects three separately
 * documented capabilities; Merchant's documented responsibility ("match known merchants,
 * create merchant records when necessary") is one operation.
 *
 * <p>Returns {@link MerchantResolutionResult}, not the domain {@code Merchant} directly - see
 * that type's Javadoc for why. Always returns a result - there is no "not found" outcome to
 * signal.
 */
public interface MerchantResolutionPort {

	MerchantResolutionResult resolve(String rawMerchantName);

}
