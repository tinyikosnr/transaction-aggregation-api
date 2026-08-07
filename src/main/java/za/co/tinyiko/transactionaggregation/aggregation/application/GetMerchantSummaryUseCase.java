package za.co.tinyiko.transactionaggregation.aggregation.application;

import java.util.List;
import java.util.UUID;

import za.co.tinyiko.transactionaggregation.aggregation.domain.DateRange;

/**
 * Inbound port for the customer's merchant breakdown (TDS 6, 34; UC-07).
 */
public interface GetMerchantSummaryUseCase {

	List<MerchantSummaryView> get(UUID customerId, DateRange dateRange);

}
