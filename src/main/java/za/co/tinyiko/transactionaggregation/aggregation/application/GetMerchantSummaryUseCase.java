package za.co.tinyiko.transactionaggregation.aggregation.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Inbound port for the customer's merchant breakdown (TDS 6, 34; UC-07). See
 * {@link GetCustomerSummaryUseCase}'s Javadoc for why this takes raw {@code from}/{@code to}
 * rather than {@code aggregation.domain.DateRange}.
 */
public interface GetMerchantSummaryUseCase {

	List<MerchantSummaryView> get(UUID customerId, LocalDate from, LocalDate to);

}
