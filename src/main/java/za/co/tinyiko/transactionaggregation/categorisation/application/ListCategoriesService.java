package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategory;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategoryId;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategoryRepositoryPort;

@Service
class ListCategoriesService implements ListCategoriesUseCase {

	private final CategoryRepositoryPort categoryRepositoryPort;

	ListCategoriesService(CategoryRepositoryPort categoryRepositoryPort) {
		this.categoryRepositoryPort = categoryRepositoryPort;
	}

	@Override
	public List<CategoryAdminView> list() {
		return categoryRepositoryPort.findAll().stream()
				.map(ListCategoriesService::toView)
				.toList();
	}

	@Override
	public CategoryAdminView get(UUID categoryId) {
		TransactionCategory category = categoryRepositoryPort.findById(new TransactionCategoryId(categoryId))
				.orElseThrow(() -> new CategoryNotFoundException(categoryId));
		return toView(category);
	}

	private static CategoryAdminView toView(TransactionCategory category) {
		return new CategoryAdminView(
				category.id().value(),
				category.code(),
				category.name(),
				category.description(),
				category.fallback(),
				category.active(),
				category.createdAt(),
				category.updatedAt());
	}

}
