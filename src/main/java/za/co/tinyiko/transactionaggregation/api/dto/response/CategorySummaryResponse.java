package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One row of the customer's category breakdown (TDS 33). {@code categoryId} stays a raw
 * {@code UUID} - no name/code enrichment, matching the decision that enrichment for this branch
 * is scoped strictly to the create-transaction response (SAD 35.1), not extended to aggregation
 * summaries, whose exact response shape TDS never documents past the raw fields
 * {@code aggregation.application.CategorySummaryView} already exposes.
 */
public record CategorySummaryResponse(UUID categoryId, BigDecimal totalAmount, String currency, long transactionCount) {
}
