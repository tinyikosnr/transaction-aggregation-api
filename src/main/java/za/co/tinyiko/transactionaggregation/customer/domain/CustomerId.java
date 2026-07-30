package za.co.tinyiko.transactionaggregation.customer.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * The identity of a {@link Customer}, per TDS 18.
 *
 * <p>Identity is generated here, via {@link #generate()}, rather than inside {@link Customer}'s
 * factory method — the aggregate is handed a fully-formed identity to construct with, it does
 * not decide how identities are produced.
 */
public record CustomerId(UUID value) {

	public CustomerId {
		Objects.requireNonNull(value, "value must not be null");
	}

	public static CustomerId generate() {
		return new CustomerId(UUID.randomUUID());
	}

}
