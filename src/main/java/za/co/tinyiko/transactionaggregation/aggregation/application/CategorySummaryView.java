package za.co.tinyiko.transactionaggregation.aggregation.application;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One row of {@link GetCategorySummaryUseCase}'s response (TDS 33: debit transactions grouped
 * by category). {@code categoryId} is a raw UUID, not {@code categorisation.domain.TransactionCategoryId}
 * - resolving it to a category name/code is a {@code feature/api} concern (see the plan's
 * documentation-conflict note on merchant/category name resolution), not this module's.
 */
public record CategorySummaryView(UUID categoryId, BigDecimal totalAmount, String currency, long transactionCount) {
}
