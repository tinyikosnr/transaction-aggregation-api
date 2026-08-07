package za.co.tinyiko.transactionaggregation.aggregation.application;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One row of {@link GetMerchantSummaryUseCase}'s response (TDS 34: debit transactions grouped
 * by merchant). Transactions with no resolved merchant are excluded upstream (see
 * {@code transaction.persistence.SpringDataTransactionSummaryRepository#merchantTotals}), not
 * represented here as a null-merchant row.
 */
public record MerchantSummaryView(UUID merchantId, BigDecimal totalAmount, String currency, long transactionCount) {
}
