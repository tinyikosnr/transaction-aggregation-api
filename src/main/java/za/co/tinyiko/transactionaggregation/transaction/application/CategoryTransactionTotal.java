package za.co.tinyiko.transactionaggregation.transaction.application;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One grouped row of {@link TransactionQueryPort#categoryTotals} (debit transactions only, per
 * TDS 33). See {@code CustomerTransactionTotals}'s Javadoc for why this is a distinct type from
 * {@code transaction.port.CategoryTotalsRow}.
 */
public record CategoryTransactionTotal(UUID categoryId, BigDecimal totalAmount, long transactionCount) {
}
