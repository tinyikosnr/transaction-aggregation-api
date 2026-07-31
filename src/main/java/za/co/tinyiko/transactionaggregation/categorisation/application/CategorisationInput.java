package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.util.Objects;

import za.co.tinyiko.transactionaggregation.categorisation.domain.Direction;

/**
 * The data {@link CategoriseTransactionUseCase} needs - deliberately primitives and
 * categorisation's own {@link Direction}, nothing from another module (no {@code MerchantId},
 * no transaction type of any kind). {@code merchantText} is nullable, matching a transaction's
 * merchant being optional when it cannot be resolved; {@code description} and {@code direction}
 * are always present on a transaction, so they're required here.
 */
public record CategorisationInput(String merchantText, String description, Direction direction) {

	public CategorisationInput {
		Objects.requireNonNull(description, "description must not be null");
		Objects.requireNonNull(direction, "direction must not be null");
	}

}
