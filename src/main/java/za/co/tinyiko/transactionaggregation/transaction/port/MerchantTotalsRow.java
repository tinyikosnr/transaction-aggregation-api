package za.co.tinyiko.transactionaggregation.transaction.port;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Internal query projection for {@link TransactionSummaryRepositoryPort#merchantTotals}. See
 * {@link CustomerTotalsRow}'s Javadoc for why this stays in {@code transaction.port} rather
 * than reusing {@code transaction.application.MerchantTransactionTotal}.
 */
public record MerchantTotalsRow(UUID merchantId, BigDecimal totalAmount, long transactionCount) {
}
