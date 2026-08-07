package za.co.tinyiko.transactionaggregation.aggregation.application;

import java.util.List;
import java.util.UUID;

import za.co.tinyiko.transactionaggregation.aggregation.domain.DateRange;

/**
 * Inbound port for the customer's monthly breakdown (TDS 6, 35; UC-08).
 */
public interface GetMonthlySummaryUseCase {

	List<MonthlySummaryView> get(UUID customerId, DateRange dateRange);

}
