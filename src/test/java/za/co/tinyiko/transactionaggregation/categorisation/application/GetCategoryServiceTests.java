package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategory;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategoryId;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategoryRepositoryPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCategoryServiceTests {

	private static final UUID CATEGORY_ID = UUID.randomUUID();

	@Mock
	private CategoryRepositoryPort categoryRepositoryPort;

	private GetCategoryService service() {
		return new GetCategoryService(categoryRepositoryPort);
	}

	private static TransactionCategory aCategory() {
		return TransactionCategory.reconstitute(new TransactionCategoryId(CATEGORY_ID),
				"GROCERIES", "Groceries", "Grocery purchases", false, true,
				Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
	}

	@Test
	void returnsIdCodeAndNameForAnExistingCategory() {
		when(categoryRepositoryPort.findById(new TransactionCategoryId(CATEGORY_ID))).thenReturn(Optional.of(aCategory()));

		CategoryView view = service().get(CATEGORY_ID);

		assertThat(view.categoryId()).isEqualTo(CATEGORY_ID);
		assertThat(view.code()).isEqualTo("GROCERIES");
		assertThat(view.name()).isEqualTo("Groceries");
	}

	@Test
	void throwsWhenCategoryDoesNotExist() {
		when(categoryRepositoryPort.findById(new TransactionCategoryId(CATEGORY_ID))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().get(CATEGORY_ID)).isInstanceOf(CategoryNotFoundException.class);
	}

	@Test
	void findIdByCodeReturnsTheMatchingId() {
		when(categoryRepositoryPort.findByCode("GROCERIES")).thenReturn(Optional.of(aCategory()));

		assertThat(service().findIdByCode("GROCERIES")).contains(CATEGORY_ID);
	}

	@Test
	void findIdByCodeReturnsEmptyForAnUnknownCode() {
		when(categoryRepositoryPort.findByCode("UNKNOWN")).thenReturn(Optional.empty());

		assertThat(service().findIdByCode("UNKNOWN")).isEmpty();
	}

	@Test
	void getByIdsReturnsAViewPerMatchedCategory() {
		when(categoryRepositoryPort.findByIds(List.of(new TransactionCategoryId(CATEGORY_ID)))).thenReturn(List.of(aCategory()));

		List<CategoryView> views = service().getByIds(List.of(CATEGORY_ID));

		assertThat(views).containsExactly(new CategoryView(CATEGORY_ID, "GROCERIES", "Groceries"));
	}

}
