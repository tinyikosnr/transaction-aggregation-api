package za.co.tinyiko.transactionaggregation.transaction.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * The identity of a {@link Transaction}, per TDS 17/59. Same pattern as every other id type in
 * this codebase: generated here, via {@link #generate()}, not inside the aggregate's factory
 * method.
 */
public record TransactionId(UUID value) {

	public TransactionId {
		Objects.requireNonNull(value, "value must not be null");
	}

	public static TransactionId generate() {
		return new TransactionId(UUID.randomUUID());
	}

}
