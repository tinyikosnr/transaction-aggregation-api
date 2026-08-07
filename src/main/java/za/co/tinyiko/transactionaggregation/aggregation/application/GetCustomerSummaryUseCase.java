package za.co.tinyiko.transactionaggregation.aggregation.application;

import java.util.UUID;

import za.co.tinyiko.transactionaggregation.aggregation.domain.DateRange;

/**
 * Inbound port for the customer financial summary (TDS 6, 32; UC-05). {@code customerId} is a
 * raw UUID, not {@code customer.domain.CustomerId} - matching the boundary-primitives pattern
 * already established for {@code CreateTransactionCommand}/{@code CustomerExistsPort}.
 */
public interface GetCustomerSummaryUseCase {

	CustomerSummaryView get(UUID customerId, DateRange dateRange);

}
