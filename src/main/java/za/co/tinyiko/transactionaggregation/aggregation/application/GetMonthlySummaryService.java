package za.co.tinyiko.transactionaggregation.aggregation.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import za.co.tinyiko.transactionaggregation.aggregation.domain.DateRange;
import za.co.tinyiko.transactionaggregation.customer.application.CustomerExistsPort;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionQueryPort;

@Service
class GetMonthlySummaryService implements GetMonthlySummaryUseCase {

	private static final String CURRENCY = "ZAR";

	private final TransactionQueryPort transactionQueryPort;
	private final CustomerExistsPort customerExistsPort;

	GetMonthlySummaryService(TransactionQueryPort transactionQueryPort, CustomerExistsPort customerExistsPort) {
		this.transactionQueryPort = transactionQueryPort;
		this.customerExistsPort = customerExistsPort;
	}

	@Override
	public List<MonthlySummaryView> get(UUID customerId, LocalDate from, LocalDate to) {
		DateRange dateRange = new DateRange(from, to);
		if (!customerExistsPort.exists(customerId)) {
			throw new CustomerNotFoundException(customerId);
		}

		return transactionQueryPort.monthlyTotals(customerId, dateRange.from(), dateRange.to()).stream()
				.map(total -> new MonthlySummaryView(
						total.month(),
						total.totalIncome(),
						total.totalExpenditure(),
						total.totalIncome().subtract(total.totalExpenditure()),
						CURRENCY,
						total.transactionCount()))
				.toList();
	}

}
