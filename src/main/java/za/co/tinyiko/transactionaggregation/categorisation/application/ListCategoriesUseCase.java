package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.util.List;
import java.util.UUID;

/**
 * Inbound port for the two read-only category-admin operations (feature/category-admin): list
 * every category, and get one by id. Read-only, deliberately: category administration in this
 * branch is an explicit, documented project decision to expose reads only (list/get) - no
 * create/update/deactivate/delete, since neither SAD nor TDS documents any category write
 * contract. {@code get} and {@code list} share one interface, the same "single
 * read-only capability, single/plural inputs" shape already used by
 * {@code merchant.application.GetMerchantsUseCase}.
 */
public interface ListCategoriesUseCase {

	List<CategoryAdminView> list();

	CategoryAdminView get(UUID categoryId);

}
