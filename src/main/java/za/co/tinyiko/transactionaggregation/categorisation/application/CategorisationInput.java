package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.util.Objects;

import za.co.tinyiko.transactionaggregation.categorisation.domain.Direction;

/**
 * The data {@link CategoriseTransactionUseCase} needs - deliberately primitives only, nothing
 * from another module (no {@code MerchantId}, no transaction type of any kind).
 * {@code merchantText} is nullable, matching a transaction's merchant being optional when it
 * cannot be resolved; {@code description} and {@code direction} are always present on a
 * transaction, so they're required here.
 *
 * <p>{@code direction} is a {@code String} ({@code "CREDIT"}/{@code "DEBIT"}), not
 * {@code categorisation.domain.Direction}: this package is exposed cross-module via
 * {@code @NamedInterface}, but {@code categorisation.domain} is not, and a caller shouldn't
 * need to reach into it just to construct this record. Validated here via
 * {@link Direction#valueOf(String)} - referencing the domain type from within this module's own
 * application package is always fine, the constraint only applies to other modules.
 */
public record CategorisationInput(String merchantText, String description, String direction) {

	public CategorisationInput {
		Objects.requireNonNull(description, "description must not be null");
		Objects.requireNonNull(direction, "direction must not be null");
		Direction.valueOf(direction);
	}

}
