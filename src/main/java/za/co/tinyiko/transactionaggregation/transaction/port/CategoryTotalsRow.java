package za.co.tinyiko.transactionaggregation.transaction.port;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Internal query projection for {@link TransactionSummaryRepositoryPort#categoryTotals}. See
 * {@link CustomerTotalsRow}'s Javadoc for why this stays in {@code transaction.port} rather
 * than reusing {@code transaction.application.CategoryTransactionTotal}.
 */
public record CategoryTotalsRow(UUID categoryId, BigDecimal totalAmount, long transactionCount) {
}
