package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategory;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategoryId;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategoryRepositoryPort;

@Service
class GetCategoryService implements GetCategoryUseCase {

	private final CategoryRepositoryPort categoryRepositoryPort;

	GetCategoryService(CategoryRepositoryPort categoryRepositoryPort) {
		this.categoryRepositoryPort = categoryRepositoryPort;
	}

	@Override
	public CategoryView get(UUID categoryId) {
		TransactionCategory category = categoryRepositoryPort.findById(new TransactionCategoryId(categoryId))
				.orElseThrow(() -> new CategoryNotFoundException(categoryId));
		return toView(category);
	}

	@Override
	public Optional<UUID> findIdByCode(String categoryCode) {
		return categoryRepositoryPort.findByCode(categoryCode)
				.map(category -> category.id().value());
	}

	@Override
	public List<CategoryView> getByIds(Collection<UUID> categoryIds) {
		List<TransactionCategoryId> typedIds = categoryIds.stream().map(TransactionCategoryId::new).toList();
		return categoryRepositoryPort.findByIds(typedIds).stream()
				.map(this::toView)
				.toList();
	}

	private CategoryView toView(TransactionCategory category) {
		return new CategoryView(category.id().value(), category.code(), category.name());
	}

}
