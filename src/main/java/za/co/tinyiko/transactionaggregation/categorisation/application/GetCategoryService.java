package za.co.tinyiko.transactionaggregation.categorisation.application;

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
		return new CategoryView(category.code(), category.name());
	}

}
