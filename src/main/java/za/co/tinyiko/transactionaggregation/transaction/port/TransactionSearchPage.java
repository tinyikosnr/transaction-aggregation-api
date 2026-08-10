package za.co.tinyiko.transactionaggregation.transaction.port;

import java.util.List;

/**
 * {@code transaction.port}'s own result shape for {@link TransactionSearchRepositoryPort#search} -
 * just the rows and the total match count; {@code transaction.application} already knows the
 * requested page/size (it supplied them via {@link TransactionSearchQuery}) and computes
 * {@code totalPages} itself, so there is nothing to echo back beyond {@code totalElements}.
 */
public record TransactionSearchPage(List<TransactionSearchRow> rows, long totalElements) {
}
