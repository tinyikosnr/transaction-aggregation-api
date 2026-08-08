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
import za.co.tinyiko.transactionaggregation.transaction.application.MerchantTransactionTotal;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionQueryPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMerchantSummaryServiceTests {

	private static final UUID CUSTOMER_ID = UUID.randomUUID();
	private static final DateRange DATE_RANGE = new DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

	@Mock
	private TransactionQueryPort transactionQueryPort;

	@Mock
	private CustomerExistsPort customerExistsPort;

	private GetMerchantSummaryService service() {
		return new GetMerchantSummaryService(transactionQueryPort, customerExistsPort);
	}

	@Test
	void throwsWhenCustomerDoesNotExist() {
		when(customerExistsPort.exists(CUSTOMER_ID)).thenReturn(false);

		assertThatThrownBy(() -> service().get(CUSTOMER_ID, DATE_RANGE.from(), DATE_RANGE.to())).isInstanceOf(CustomerNotFoundException.class);
	}

	@Test
	void mapsEachMerchantTotalToAView() {
		UUID merchantId = UUID.randomUUID();
		when(customerExistsPort.exists(CUSTOMER_ID)).thenReturn(true);
		when(transactionQueryPort.merchantTotals(CUSTOMER_ID, DATE_RANGE.from(), DATE_RANGE.to()))
				.thenReturn(List.of(new MerchantTransactionTotal(merchantId, new BigDecimal("100.00"), 1)));

		List<MerchantSummaryView> views = service().get(CUSTOMER_ID, DATE_RANGE.from(), DATE_RANGE.to());

		assertThat(views).containsExactly(new MerchantSummaryView(merchantId, new BigDecimal("100.00"), "ZAR", 1));
	}

	@Test
	void returnsEmptyListWhenNoMerchantTotals() {
		when(customerExistsPort.exists(CUSTOMER_ID)).thenReturn(true);
		when(transactionQueryPort.merchantTotals(CUSTOMER_ID, DATE_RANGE.from(), DATE_RANGE.to())).thenReturn(List.of());

		assertThat(service().get(CUSTOMER_ID, DATE_RANGE.from(), DATE_RANGE.to())).isEmpty();
	}

}
