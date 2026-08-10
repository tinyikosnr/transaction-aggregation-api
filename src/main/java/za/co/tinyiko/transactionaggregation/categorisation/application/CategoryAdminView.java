package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.time.Instant;
import java.util.UUID;

/**
 * The admin read shape of a category (feature/category-admin, SAD 27.5's full attribute list) -
 * deliberately distinct from {@link CategoryView}, not a superset extension of it: {@link
 * CategoryView} is {@link GetCategoryUseCase}'s minimal cross-module correlation shape (id, code,
 * name only), already consumed by {@code transaction} for create/search enrichment; this type is
 * {@link ListCategoriesUseCase}'s own, admin-only shape, carrying every field the category-admin
 * read endpoints expose ({@code description}, {@code fallback}, {@code active}, timestamps) that
 * enrichment callers never asked for and don't need. Kept separate for the same reason
 * {@code TransactionDetails} stays separate from {@code TransactionCreatedResult}: different
 * capability, different caller, no architectural reason strong enough to unify beyond eliminating
 * a handful of duplicate fields.
 */
public record CategoryAdminView(
		UUID id,
		String code,
		String name,
		String description,
		boolean fallback,
		boolean active,
		Instant createdAt,
		Instant updatedAt
) {
}
