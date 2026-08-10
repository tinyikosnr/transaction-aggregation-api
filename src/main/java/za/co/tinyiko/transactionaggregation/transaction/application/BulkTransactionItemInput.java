package za.co.tinyiko.transactionaggregation.transaction.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One raw, unvalidated item from a bulk create request (SAD 35.2, TDS 29). Deliberately distinct
 * from {@link CreateTransactionCommand}: every field here may be {@code null}, since a
 * structurally-invalid item must become a per-item {@link BulkTransactionItemResult} failure
 * (SAD 32.5/39.8: one invalid item must not roll back the rest of the batch), not an exception
 * thrown while merely constructing the input list. {@link CreateTransactionCommand}'s own
 * {@code Objects.requireNonNull} invariants stay exactly as strict as they've always been -
 * {@code BulkCreateTransactionsService} validates a {@code BulkTransactionItemInput} itself,
 * translating a missing/invalid required field into a {@link TransactionValidationException}
 * (the same type every other per-item validation failure already uses), before ever constructing
 * a {@link CreateTransactionCommand}.
 */
public record BulkTransactionItemInput(
		String externalTransactionId,
		UUID customerId,
		String sourceCode,
		BigDecimal amount,
		String currency,
		String direction,
		String description,
		String merchantName,
		Instant occurredAt
) {
}
