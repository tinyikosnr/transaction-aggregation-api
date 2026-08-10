package za.co.tinyiko.transactionaggregation.transaction.application;

import java.util.List;
import java.util.Objects;

import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;

/**
 * The data required to process a bulk create request (SAD 35.2, TDS 29). {@code correlationId}
 * and {@code actor} are shared across every item in the batch - one authenticated caller, one
 * inbound HTTP request, resolved once by the caller exactly as for
 * {@link CreateTransactionCommand}, not invented per item (there is no documented per-item
 * correlation id).
 */
public record BulkCreateTransactionsCommand(
		List<BulkTransactionItemInput> items,
		CorrelationId correlationId,
		String actor
) {

	public BulkCreateTransactionsCommand {
		Objects.requireNonNull(items, "items must not be null");
		Objects.requireNonNull(correlationId, "correlationId must not be null");
		Objects.requireNonNull(actor, "actor must not be null");
	}

}
