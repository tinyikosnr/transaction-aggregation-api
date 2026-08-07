package za.co.tinyiko.transactionaggregation.aggregation.application;

import java.util.List;
import java.util.UUID;

import za.co.tinyiko.transactionaggregation.aggregation.domain.DateRange;

/**
 * Inbound port for the customer's category breakdown (TDS 6, 33; UC-06).
 */
public interface GetCategorySummaryUseCase {

	List<CategorySummaryView> get(UUID customerId, DateRange dateRange);

}
