package za.co.tinyiko.transactionaggregation.transaction.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.tinyiko.transactionaggregation.transaction.port.CategoryTotalsRow;
import za.co.tinyiko.transactionaggregation.transaction.port.CustomerTotalsRow;
import za.co.tinyiko.transactionaggregation.transaction.port.MerchantTotalsRow;
import za.co.tinyiko.transactionaggregation.transaction.port.MonthlyTotalsRow;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSummaryRepositoryPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionQueryServiceTests {

	private static final UUID CUSTOMER_ID = UUID.randomUUID();
	private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
	private static final LocalDate TO = LocalDate.of(2026, 1, 31);

	@Mock
	private TransactionSummaryRepositoryPort transactionSummaryRepositoryPort;

	private TransactionQueryService service() {
		return new TransactionQueryService(transactionSummaryRepositoryPort);
	}

	@Test
	void translatesTheLocalDateRangeToAnInclusiveLowerExclusiveUpperUtcInstantRange() {
		when(transactionSummaryRepositoryPort.customerTotals(eq(CUSTOMER_ID), any(), any()))
				.thenReturn(new CustomerTotalsRow(BigDecimal.ZERO, BigDecimal.ZERO, 0));

		service().customerTotals(CUSTOMER_ID, FROM, TO);

		ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.forClass(Instant.class);
		ArgumentCaptor<Instant> toCaptor = ArgumentCaptor.forClass(Instant.class);
		verify(transactionSummaryRepositoryPort).customerTotals(eq(CUSTOMER_ID), fromCaptor.capture(), toCaptor.capture());
		assertThat(fromCaptor.getValue()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
		assertThat(toCaptor.getValue()).isEqualTo(Instant.parse("2026-02-01T00:00:00Z"));
	}

	@Test
	void mapsCustomerTotalsRowToThePublicRecord() {
		when(transactionSummaryRepositoryPort.customerTotals(eq(CUSTOMER_ID), any(), any()))
				.thenReturn(new CustomerTotalsRow(new BigDecimal("100.00"), new BigDecimal("40.00"), 3));

		CustomerTransactionTotals totals = service().customerTotals(CUSTOMER_ID, FROM, TO);

		assertThat(totals.totalIncome()).isEqualByComparingTo("100.00");
		assertThat(totals.totalExpenditure()).isEqualByComparingTo("40.00");
		assertThat(totals.transactionCount()).isEqualTo(3);
	}

	@Test
	void mapsCategoryTotalsRowsToThePublicRecords() {
		UUID categoryId = UUID.randomUUID();
		when(transactionSummaryRepositoryPort.categoryTotals(eq(CUSTOMER_ID), any(), any()))
				.thenReturn(List.of(new CategoryTotalsRow(categoryId, new BigDecimal("50.00"), 2)));

		List<CategoryTransactionTotal> totals = service().categoryTotals(CUSTOMER_ID, FROM, TO);

		assertThat(totals).containsExactly(new CategoryTransactionTotal(categoryId, new BigDecimal("50.00"), 2));
	}

	@Test
	void mapsMerchantTotalsRowsToThePublicRecords() {
		UUID merchantId = UUID.randomUUID();
		when(transactionSummaryRepositoryPort.merchantTotals(eq(CUSTOMER_ID), any(), any()))
				.thenReturn(List.of(new MerchantTotalsRow(merchantId, new BigDecimal("25.00"), 1)));

		List<MerchantTransactionTotal> totals = service().merchantTotals(CUSTOMER_ID, FROM, TO);

		assertThat(totals).containsExactly(new MerchantTransactionTotal(merchantId, new BigDecimal("25.00"), 1));
	}

	@Test
	void mapsMonthlyTotalsRowsToThePublicRecords() {
		YearMonth month = YearMonth.of(2026, 1);
		when(transactionSummaryRepositoryPort.monthlyTotals(eq(CUSTOMER_ID), any(), any()))
				.thenReturn(List.of(new MonthlyTotalsRow(month, new BigDecimal("200.00"), new BigDecimal("75.00"), 4)));

		List<MonthlyTransactionTotal> totals = service().monthlyTotals(CUSTOMER_ID, FROM, TO);

		assertThat(totals).containsExactly(new MonthlyTransactionTotal(month, new BigDecimal("200.00"), new BigDecimal("75.00"), 4));
	}

}
