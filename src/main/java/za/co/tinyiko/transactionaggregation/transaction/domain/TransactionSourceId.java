package za.co.tinyiko.transactionaggregation.transaction.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * The identity of a {@link TransactionSource}. Same pattern as every other id type in this
 * codebase: generated here, via {@link #generate()}, not inside the aggregate's factory method.
 */
public record TransactionSourceId(UUID value) {

	public TransactionSourceId {
		Objects.requireNonNull(value, "value must not be null");
	}

	public static TransactionSourceId generate() {
		return new TransactionSourceId(UUID.randomUUID());
	}

}
