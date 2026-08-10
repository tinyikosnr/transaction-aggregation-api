package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
class ListCategoriesServiceTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-10T08:15:00Z"), ZoneOffset.UTC);
	private static final UUID CATEGORY_ID = UUID.randomUUID();

	@Mock
	private CategoryRepositoryPort categoryRepositoryPort;

	private ListCategoriesService service() {
		return new ListCategoriesService(categoryRepositoryPort);
	}

	private static TransactionCategory aCategory() {
		return TransactionCategory.register(new TransactionCategoryId(CATEGORY_ID), "GROCERIES", "Groceries", "desc", false, FIXED_CLOCK);
	}

	@Test
	void listReturnsAViewPerCategory() {
		when(categoryRepositoryPort.findAll()).thenReturn(List.of(aCategory()));

		List<CategoryAdminView> views = service().list();

		assertThat(views).hasSize(1);
		assertThat(views.get(0).id()).isEqualTo(CATEGORY_ID);
		assertThat(views.get(0).code()).isEqualTo("GROCERIES");
		assertThat(views.get(0).description()).isEqualTo("desc");
		assertThat(views.get(0).fallback()).isFalse();
		assertThat(views.get(0).active()).isTrue();
	}

	@Test
	void getReturnsTheMatchingCategory() {
		when(categoryRepositoryPort.findById(new TransactionCategoryId(CATEGORY_ID))).thenReturn(Optional.of(aCategory()));

		CategoryAdminView view = service().get(CATEGORY_ID);

		assertThat(view.id()).isEqualTo(CATEGORY_ID);
	}

	@Test
	void getThrowsWhenCategoryDoesNotExist() {
		when(categoryRepositoryPort.findById(new TransactionCategoryId(CATEGORY_ID))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().get(CATEGORY_ID)).isInstanceOf(CategoryNotFoundException.class);
	}

}
