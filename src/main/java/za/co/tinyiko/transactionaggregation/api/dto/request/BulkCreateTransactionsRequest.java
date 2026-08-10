package za.co.tinyiko.transactionaggregation.api.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The bulk-create request body (SAD 35.2, TDS 29) - a wrapper object, not a raw JSON array,
 * matching TDS 55's own dedicated {@code BulkCreateTransactionsRequest} class and this codebase's
 * established convention of every write endpoint having its own request DTO.
 *
 * <p>{@code transactions} is deliberately <strong>not</strong> annotated {@code @Valid}: cascading
 * Bean Validation into every element would fail the whole request the moment any single item
 * violates a field constraint, rejecting every valid item too - directly contradicting SAD
 * 32.5/39.8's partial-success requirement ("one invalid item must not roll back the rest of the
 * batch"). Per-item structural/business validation is instead owned entirely by {@code
 * transaction.application.BulkCreateTransactionsService}, which reports each invalid item as its
 * own {@code REQUEST_VALIDATION_FAILED} result rather than rejecting the request. Only
 * whole-request constraints (empty batch, batch larger than the documented maximum) are enforced
 * here, since a malformed envelope genuinely has nothing to process at all.
 */
public record BulkCreateTransactionsRequest(
		@Schema(description = "Transactions to create in this batch (1-500 items), processed independently: one invalid item is reported as its own failed result and never rolls back the rest (no atomic all-or-nothing guarantee).",
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotEmpty(message = "must contain at least one transaction")
		@Size(max = 500, message = "must not contain more than 500 transactions")
		List<@NotNull(message = "must not contain a null transaction") CreateTransactionRequest> transactions
) {
}
