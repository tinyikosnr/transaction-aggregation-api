package za.co.tinyiko.transactionaggregation.aggregation.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Inbound port for the customer's category breakdown (TDS 6, 33; UC-06). See
 * {@link GetCustomerSummaryUseCase}'s Javadoc for why this takes raw {@code from}/{@code to}
 * rather than {@code aggregation.domain.DateRange}.
 */
public interface GetCategorySummaryUseCase {

	List<CategorySummaryView> get(UUID customerId, LocalDate from, LocalDate to);

}
