package za.co.tinyiko.transactionaggregation.categorisation.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * The identity of a {@link TransactionCategory}. Same pattern as {@code CustomerId}/
 * {@code MerchantId}: generated here, via {@link #generate()}, not inside the aggregate's
 * factory method.
 */
public record TransactionCategoryId(UUID value) {

	public TransactionCategoryId {
		Objects.requireNonNull(value, "value must not be null");
	}

	public static TransactionCategoryId generate() {
		return new TransactionCategoryId(UUID.randomUUID());
	}

}
