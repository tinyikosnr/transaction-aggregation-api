package za.co.tinyiko.transactionaggregation.transaction.application;

import java.time.Instant;
import java.util.UUID;

/**
 * The inbound criteria for {@link SearchTransactionsUseCase} (SAD 34.7, TDS §31). Every filter
 * field is independently optional. {@code occurredFrom}/{@code occurredTo} filter
 * {@code occurredAt} (the business event time, matching SAD's own {@code occurredFrom}/
 * {@code occurredTo} naming - not {@code receivedAt}), both bounds inclusive when supplied.
 * {@code sort} is the raw, unparsed query value (for example {@code "transactionTimestamp,desc"})
 * - {@link SearchTransactionsService} owns splitting and whitelisting it, the same place every
 * other structural validation for this use case lives.
 */
public record TransactionSearchCriteria(
		UUID customerId,
		String sourceCode,
		String categoryCode,
		UUID merchantId,
		String direction,
		String status,
		Instant occurredFrom,
		Instant occurredTo,
		int page,
		int size,
		String sort
) {
}
