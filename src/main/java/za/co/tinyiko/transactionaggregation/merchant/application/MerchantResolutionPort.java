package za.co.tinyiko.transactionaggregation.merchant.application;

import za.co.tinyiko.transactionaggregation.merchant.domain.Merchant;

/**
 * Inbound port other modules use to resolve a raw merchant name into a normalised
 * {@link Merchant}, matching an existing one or creating a new one as needed (TDS 3, 60).
 *
 * <p>A single, fused capability — deliberately not split into separate lookup/exists/register
 * ports the way {@code customer} was. Customer's three-port split reflects three separately
 * documented capabilities; Merchant's documented responsibility ("match known merchants,
 * create merchant records when necessary") is one operation.
 *
 * <p>Always returns a {@link Merchant} — there is no "not found" outcome to signal.
 */
public interface MerchantResolutionPort {

	Merchant resolve(String rawMerchantName);

}
