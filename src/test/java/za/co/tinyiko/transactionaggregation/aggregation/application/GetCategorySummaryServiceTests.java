package za.co.tinyiko.transactionaggregation.aggregation.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.tinyiko.transactionaggregation.aggregation.domain.DateRange;
import za.co.tinyiko.transactionaggregation.customer.application.CustomerExistsPort;
import za.co.tinyiko.transactionaggregation.transaction.application.CategoryTransactionTotal;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionQueryPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCategorySummaryServiceTests {

	private static final UUID CUSTOMER_ID = UUID.randomUUID();
	private static final DateRange DATE_RANGE = new DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

	@Mock
	private TransactionQueryPort transactionQueryPort;

	@Mock
	private CustomerExistsPort customerExistsPort;

	private GetCategorySummaryService service() {
		return new GetCategorySummaryService(transactionQueryPort, customerExistsPort);
	}

	@Test
	void throwsWhenCustomerDoesNotExist() {
		when(customerExistsPort.exists(CUSTOMER_ID)).thenReturn(false);

		assertThatThrownBy(() -> service().get(CUSTOMER_ID, DATE_RANGE.from(), DATE_RANGE.to())).isInstanceOf(CustomerNotFoundException.class);
	}

	@Test
	void mapsEachCategoryTotalToAView() {
		UUID categoryId = UUID.randomUUID();
		when(customerExistsPort.exists(CUSTOMER_ID)).thenReturn(true);
		when(transactionQueryPort.categoryTotals(CUSTOMER_ID, DATE_RANGE.from(), DATE_RANGE.to()))
				.thenReturn(List.of(new CategoryTransactionTotal(categoryId, new BigDecimal("130.00"), 2)));

		List<CategorySummaryView> views = service().get(CUSTOMER_ID, DATE_RANGE.from(), DATE_RANGE.to());

		assertThat(views).containsExactly(new CategorySummaryView(categoryId, new BigDecimal("130.00"), "ZAR", 2));
	}

	@Test
	void returnsEmptyListWhenNoCategoryTotals() {
		when(customerExistsPort.exists(CUSTOMER_ID)).thenReturn(true);
		when(transactionQueryPort.categoryTotals(CUSTOMER_ID, DATE_RANGE.from(), DATE_RANGE.to())).thenReturn(List.of());

		assertThat(service().get(CUSTOMER_ID, DATE_RANGE.from(), DATE_RANGE.to())).isEmpty();
	}

}
