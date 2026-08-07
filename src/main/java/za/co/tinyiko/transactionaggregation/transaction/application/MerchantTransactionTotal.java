package za.co.tinyiko.transactionaggregation.transaction.application;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One grouped row of {@link TransactionQueryPort#merchantTotals} (debit transactions with a
 * resolved merchant only, per TDS 34). See {@code CustomerTransactionTotals}'s Javadoc for why
 * this is a distinct type from {@code transaction.port.MerchantTotalsRow}.
 */
public record MerchantTransactionTotal(UUID merchantId, BigDecimal totalAmount, long transactionCount) {
}
