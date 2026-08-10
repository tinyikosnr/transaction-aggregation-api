package za.co.tinyiko.transactionaggregation.transaction.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One row of {@link SearchTransactionsUseCase}'s response, matching SAD 35.4's documented search
 * row exactly: {@code merchantName} (a plain display-name string, not an id/object), and
 * {@code categoryCode} only (no category name) - deliberately narrower than
 * {@link TransactionDetails}, since the response never surfaces the extra fields.
 * {@code merchantName} is {@code null} when the transaction has no resolved merchant.
 */
public record TransactionSearchResultItem(
		UUID id,
		UUID customerId,
		String merchantName,
		String categoryCode,
		BigDecimal amount,
		String currency,
		String direction,
		Instant occurredAt
) {
}
