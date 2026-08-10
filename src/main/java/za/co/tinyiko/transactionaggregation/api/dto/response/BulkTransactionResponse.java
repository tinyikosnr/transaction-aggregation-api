package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.util.List;

/**
 * The bulk-create response envelope, matching SAD 35.2's documented shape exactly ({@code total}/
 * {@code successful}/{@code failed}/{@code results}). Returned with {@code 207 Multi-Status} for
 * every request that reaches per-item processing, whether every item succeeded, every item
 * failed, or the outcome was mixed - SAD 39.3 documents exactly one bulk-specific status, with no
 * further split.
 */
public record BulkTransactionResponse(int total, int successful, int failed, List<BulkTransactionItemResponse> results) {
}
