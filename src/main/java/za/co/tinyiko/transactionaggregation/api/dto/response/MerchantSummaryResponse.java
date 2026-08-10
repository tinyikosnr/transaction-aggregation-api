package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One row of the customer's merchant breakdown (TDS 34). {@code merchantId} stays a raw
 * {@code UUID} - see {@link CategorySummaryResponse}'s Javadoc for why no name enrichment is
 * applied here.
 */
@Schema(description = "Total spend at one merchant within the requested date range.")
public record MerchantSummaryResponse(
		@Schema(description = "Merchant identifier (not enriched with display name).") UUID merchantId,
		@Schema(description = "Total amount for this merchant.") BigDecimal totalAmount,
		@Schema(description = "Three-letter uppercase ISO 4217 currency code.", example = "ZAR") String currency,
		@Schema(description = "Number of transactions at this merchant.") long transactionCount
) {
}
