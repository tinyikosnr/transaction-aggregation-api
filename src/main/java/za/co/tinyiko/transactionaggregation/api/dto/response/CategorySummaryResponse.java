package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One row of the customer's category breakdown (TDS 33). {@code categoryId} stays a raw
 * {@code UUID} - no name/code enrichment, matching the decision that enrichment for this branch
 * is scoped strictly to the create-transaction response (SAD 35.1), not extended to aggregation
 * summaries, whose exact response shape TDS never documents past the raw fields
 * {@code aggregation.application.CategorySummaryView} already exposes.
 */
@Schema(description = "Total spend for one category within the requested date range.")
public record CategorySummaryResponse(
		@Schema(description = "Category identifier (not enriched with code/name).") UUID categoryId,
		@Schema(description = "Total amount for this category.") BigDecimal totalAmount,
		@Schema(description = "Three-letter uppercase ISO 4217 currency code.", example = "ZAR") String currency,
		@Schema(description = "Number of transactions in this category.") long transactionCount
) {
}
