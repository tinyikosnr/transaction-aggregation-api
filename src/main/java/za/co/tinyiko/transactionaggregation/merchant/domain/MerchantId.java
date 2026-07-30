package za.co.tinyiko.transactionaggregation.merchant.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * The identity of a {@link Merchant}, per TDS 59. Same pattern as
 * {@code customer.domain.CustomerId}: generated here, via {@link #generate()}, not inside the
 * aggregate's factory method.
 */
public record MerchantId(UUID value) {

	public MerchantId {
		Objects.requireNonNull(value, "value must not be null");
	}

	public static MerchantId generate() {
		return new MerchantId(UUID.randomUUID());
	}

}
