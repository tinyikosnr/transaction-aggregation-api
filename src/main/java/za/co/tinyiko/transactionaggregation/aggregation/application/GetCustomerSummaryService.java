package za.co.tinyiko.transactionaggregation.aggregation.application;

import java.util.UUID;

import org.springframework.stereotype.Service;

import za.co.tinyiko.transactionaggregation.aggregation.domain.DateRange;
import za.co.tinyiko.transactionaggregation.customer.application.CustomerExistsPort;
import za.co.tinyiko.transactionaggregation.transaction.application.CustomerTransactionTotals;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionQueryPort;

@Service
class GetCustomerSummaryService implements GetCustomerSummaryUseCase {

	private static final String CURRENCY = "ZAR";

	private final TransactionQueryPort transactionQueryPort;
	private final CustomerExistsPort customerExistsPort;

	GetCustomerSummaryService(TransactionQueryPort transactionQueryPort, CustomerExistsPort customerExistsPort) {
		this.transactionQueryPort = transactionQueryPort;
		this.customerExistsPort = customerExistsPort;
	}

	@Override
	public CustomerSummaryView get(UUID customerId, DateRange dateRange) {
		if (!customerExistsPort.exists(customerId)) {
			throw new CustomerNotFoundException(customerId);
		}

		CustomerTransactionTotals totals = transactionQueryPort.customerTotals(customerId, dateRange.from(), dateRange.to());
		return new CustomerSummaryView(
				customerId,
				dateRange.from(),
				dateRange.to(),
				totals.totalIncome(),
				totals.totalExpenditure(),
				totals.totalIncome().subtract(totals.totalExpenditure()),
				CURRENCY,
				totals.transactionCount());
	}

}
