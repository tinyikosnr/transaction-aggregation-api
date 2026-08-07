package za.co.tinyiko.transactionaggregation.aggregation.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.tinyiko.transactionaggregation.aggregation.domain.DateRange;
import za.co.tinyiko.transactionaggregation.customer.application.CustomerExistsPort;
import za.co.tinyiko.transactionaggregation.transaction.application.CustomerTransactionTotals;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionQueryPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCustomerSummaryServiceTests {

	private static final UUID CUSTOMER_ID = UUID.randomUUID();
	private static final DateRange DATE_RANGE = new DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

	@Mock
	private TransactionQueryPort transactionQueryPort;

	@Mock
	private CustomerExistsPort customerExistsPort;

	private GetCustomerSummaryService service() {
		return new GetCustomerSummaryService(transactionQueryPort, customerExistsPort);
	}

	@Test
	void throwsWhenCustomerDoesNotExist() {
		when(customerExistsPort.exists(CUSTOMER_ID)).thenReturn(false);

		assertThatThrownBy(() -> service().get(CUSTOMER_ID, DATE_RANGE)).isInstanceOf(CustomerNotFoundException.class);
	}

	@Test
	void computesPositiveNetCashFlowWhenIncomeExceedsExpenditure() {
		when(customerExistsPort.exists(CUSTOMER_ID)).thenReturn(true);
		when(transactionQueryPort.customerTotals(CUSTOMER_ID, DATE_RANGE.from(), DATE_RANGE.to()))
				.thenReturn(new CustomerTransactionTotals(new BigDecimal("5000.00"), new BigDecimal("3212.50"), 82));

		CustomerSummaryView view = service().get(CUSTOMER_ID, DATE_RANGE);

		assertThat(view.customerId()).isEqualTo(CUSTOMER_ID);
		assertThat(view.periodFrom()).isEqualTo(DATE_RANGE.from());
		assertThat(view.periodTo()).isEqualTo(DATE_RANGE.to());
		assertThat(view.totalIncome()).isEqualByComparingTo("5000.00");
		assertThat(view.totalExpenditure()).isEqualByComparingTo("3212.50");
		assertThat(view.netCashFlow()).isEqualByComparingTo("1787.50");
		assertThat(view.currency()).isEqualTo("ZAR");
		assertThat(view.transactionCount()).isEqualTo(82);
	}

	@Test
	void computesNegativeNetCashFlowWhenExpenditureExceedsIncome() {
		when(customerExistsPort.exists(CUSTOMER_ID)).thenReturn(true);
		when(transactionQueryPort.customerTotals(CUSTOMER_ID, DATE_RANGE.from(), DATE_RANGE.to()))
				.thenReturn(new CustomerTransactionTotals(new BigDecimal("100.00"), new BigDecimal("400.00"), 5));

		CustomerSummaryView view = service().get(CUSTOMER_ID, DATE_RANGE);

		assertThat(view.netCashFlow()).isEqualByComparingTo("-300.00");
	}

	@Test
	void returnsAllZeroViewForACustomerWithNoTransactionsInRange() {
		when(customerExistsPort.exists(CUSTOMER_ID)).thenReturn(true);
		when(transactionQueryPort.customerTotals(CUSTOMER_ID, DATE_RANGE.from(), DATE_RANGE.to()))
				.thenReturn(new CustomerTransactionTotals(BigDecimal.ZERO, BigDecimal.ZERO, 0));

		CustomerSummaryView view = service().get(CUSTOMER_ID, DATE_RANGE);

		assertThat(view.totalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(view.totalExpenditure()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(view.netCashFlow()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(view.transactionCount()).isZero();
	}

}
