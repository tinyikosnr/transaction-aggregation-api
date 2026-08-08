package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.util.UUID;

/**
 * Thrown by {@link GetCategoryUseCase#get} when no category exists for the given id (TDS 40's
 * {@code CAT-001}). In practice this should never happen - the id always comes from a
 * {@link CategorisationDecision} moments earlier in the same request, which only ever assigns a
 * real, existing category id - but a defensive, named failure path is cheap and consistent with
 * how other "should not happen in practice" edge cases are handled elsewhere in this codebase.
 */
public class CategoryNotFoundException extends RuntimeException {

	public CategoryNotFoundException(UUID categoryId) {
		super("Category not found: " + categoryId);
	}

}
