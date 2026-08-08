package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One row of the customer's merchant breakdown (TDS 34). {@code merchantId} stays a raw
 * {@code UUID} - see {@link CategorySummaryResponse}'s Javadoc for why no name enrichment is
 * applied here.
 */
public record MerchantSummaryResponse(UUID merchantId, BigDecimal totalAmount, String currency, long transactionCount) {
}
