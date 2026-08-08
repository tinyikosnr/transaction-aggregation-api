package za.co.tinyiko.transactionaggregation.aggregation.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import za.co.tinyiko.transactionaggregation.aggregation.domain.DateRange;
import za.co.tinyiko.transactionaggregation.customer.application.CustomerExistsPort;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionQueryPort;

@Service
class GetCategorySummaryService implements GetCategorySummaryUseCase {

	private static final String CURRENCY = "ZAR";

	private final TransactionQueryPort transactionQueryPort;
	private final CustomerExistsPort customerExistsPort;

	GetCategorySummaryService(TransactionQueryPort transactionQueryPort, CustomerExistsPort customerExistsPort) {
		this.transactionQueryPort = transactionQueryPort;
		this.customerExistsPort = customerExistsPort;
	}

	@Override
	public List<CategorySummaryView> get(UUID customerId, LocalDate from, LocalDate to) {
		DateRange dateRange = new DateRange(from, to);
		if (!customerExistsPort.exists(customerId)) {
			throw new CustomerNotFoundException(customerId);
		}

		return transactionQueryPort.categoryTotals(customerId, dateRange.from(), dateRange.to()).stream()
				.map(total -> new CategorySummaryView(total.categoryId(), total.totalAmount(), CURRENCY, total.transactionCount()))
				.toList();
	}

}
