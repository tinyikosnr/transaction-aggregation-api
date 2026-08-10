package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.util.Objects;
import java.util.UUID;

/**
 * The response shape of {@link GetCategoryUseCase}. {@code categoryId} was added in
 * {@code feature/transaction-query} for {@link GetCategoryUseCase#getByIds}, whose batch callers
 * (unlike the original single-item {@link GetCategoryUseCase#get}) need the id echoed back to
 * correlate each result with the row it enriches - a backward-compatible superset, not a
 * conflation of two different shapes: it is still exactly "the projected view of one category",
 * just now carrying the id it always logically had.
 */
public record CategoryView(UUID categoryId, String code, String name) {

	public CategoryView {
		Objects.requireNonNull(categoryId, "categoryId must not be null");
		Objects.requireNonNull(code, "code must not be null");
		Objects.requireNonNull(name, "name must not be null");
	}

}
