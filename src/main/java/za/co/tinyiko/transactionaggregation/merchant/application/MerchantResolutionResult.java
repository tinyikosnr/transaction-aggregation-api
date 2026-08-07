package za.co.tinyiko.transactionaggregation.merchant.application;

import java.util.Objects;
import java.util.UUID;

/**
 * The outcome of {@link MerchantResolutionPort#resolve}, carrying only what a cross-module
 * caller actually needs: the resolved merchant's id (to store as a foreign key) and its
 * display text (e.g. for categorisation matching). Deliberately not the domain {@code Merchant}
 * itself - this package is exposed cross-module via {@code @NamedInterface}, but
 * {@code merchant.domain} is not, and returning {@code Merchant} directly would force every
 * caller to reference it just to read two fields off it.
 */
public record MerchantResolutionResult(UUID merchantId, String displayName) {

	public MerchantResolutionResult {
		Objects.requireNonNull(merchantId, "merchantId must not be null");
		Objects.requireNonNull(displayName, "displayName must not be null");
	}

}
