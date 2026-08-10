package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The bulk-create response envelope, matching SAD 35.2's documented shape exactly ({@code total}/
 * {@code successful}/{@code failed}/{@code results}). Returned with {@code 207 Multi-Status} for
 * every request that reaches per-item processing, whether every item succeeded, every item
 * failed, or the outcome was mixed - SAD 39.3 documents exactly one bulk-specific status, with no
 * further split.
 */
@Schema(description = "Bulk-create outcome. Always returned with 207 Multi-Status once per-item processing starts, regardless of how many items succeeded - there is no separate all-success/all-failure status.")
public record BulkTransactionResponse(
		@Schema(description = "Total number of items submitted in the batch.") int total,
		@Schema(description = "Number of items created successfully.") int successful,
		@Schema(description = "Number of items that failed (validation, duplicate, or other error).") int failed,
		@Schema(description = "Per-item outcome, in the same order as the submitted batch.") List<BulkTransactionItemResponse> results
) {
}
