package za.co.tinyiko.transactionaggregation.aggregation.application;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Inbound port for the customer financial summary (TDS 6, 32; UC-05). {@code customerId} is a
 * raw UUID, not {@code customer.domain.CustomerId} - matching the boundary-primitives pattern
 * already established for {@code CreateTransactionCommand}/{@code CustomerExistsPort}.
 *
 * <p>Takes raw {@code from}/{@code to} rather than {@code aggregation.domain.DateRange} -
 * {@code feature/api} is this port's first real external caller, and {@code aggregation.domain}
 * is not exposed cross-module (only this package carries a {@code @NamedInterface}), so the
 * implementation constructs and validates the {@code DateRange} internally instead.
 */
public interface GetCustomerSummaryUseCase {

	CustomerSummaryView get(UUID customerId, LocalDate from, LocalDate to);

}
