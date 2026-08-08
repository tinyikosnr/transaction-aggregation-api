package za.co.tinyiko.transactionaggregation.aggregation.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.tinyiko.transactionaggregation.aggregation.domain.DateRange;
import za.co.tinyiko.transactionaggregation.customer.application.CustomerExistsPort;
import za.co.tinyiko.transactionaggregation.transaction.application.MonthlyTransactionTotal;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionQueryPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMonthlySummaryServiceTests {

	private static final UUID CUSTOMER_ID = UUID.randomUUID();
	private static final DateRange DATE_RANGE = new DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 28));

	@Mock
	private TransactionQueryPort transactionQueryPort;

	@Mock
	private CustomerExistsPort customerExistsPort;

	private GetMonthlySummaryService service() {
		return new GetMonthlySummaryService(transactionQueryPort, customerExistsPort);
	}

	@Test
	void throwsWhenCustomerDoesNotExist() {
		when(customerExistsPort.exists(CUSTOMER_ID)).thenReturn(false);

		assertThatThrownBy(() -> service().get(CUSTOMER_ID, DATE_RANGE.from(), DATE_RANGE.to())).isInstanceOf(CustomerNotFoundException.class);
	}

	@Test
	void mapsEachMonthlyTotalToAViewWithComputedNetCashFlow() {
		when(customerExistsPort.exists(CUSTOMER_ID)).thenReturn(true);
		when(transactionQueryPort.monthlyTotals(CUSTOMER_ID, DATE_RANGE.from(), DATE_RANGE.to()))
				.thenReturn(List.of(new MonthlyTransactionTotal(YearMonth.of(2026, 1), new BigDecimal("5000.00"), new BigDecimal("100.00"), 2)));

		List<MonthlySummaryView> views = service().get(CUSTOMER_ID, DATE_RANGE.from(), DATE_RANGE.to());

		assertThat(views).hasSize(1);
		MonthlySummaryView view = views.get(0);
		assertThat(view.month()).isEqualTo(YearMonth.of(2026, 1));
		assertThat(view.totalIncome()).isEqualByComparingTo("5000.00");
		assertThat(view.totalExpenditure()).isEqualByComparingTo("100.00");
		assertThat(view.netCashFlow()).isEqualByComparingTo("4900.00");
		assertThat(view.currency()).isEqualTo("ZAR");
		assertThat(view.transactionCount()).isEqualTo(2);
	}

	@Test
	void returnsEmptyListWhenNoMonthlyTotals() {
		when(customerExistsPort.exists(CUSTOMER_ID)).thenReturn(true);
		when(transactionQueryPort.monthlyTotals(CUSTOMER_ID, DATE_RANGE.from(), DATE_RANGE.to())).thenReturn(List.of());

		assertThat(service().get(CUSTOMER_ID, DATE_RANGE.from(), DATE_RANGE.to())).isEmpty();
	}

}
