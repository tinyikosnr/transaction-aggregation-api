package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.util.Objects;

/**
 * The response shape of {@link GetCategoryUseCase}. Just {@code code}/{@code name}: the only
 * caller (a create-transaction response, per SAD 35.1) already knows the category id it looked
 * up, so there is no reason to echo it back here.
 */
public record CategoryView(String code, String name) {

	public CategoryView {
		Objects.requireNonNull(code, "code must not be null");
		Objects.requireNonNull(name, "name must not be null");
	}

}
