package za.co.tinyiko.transactionaggregation.transaction.application;

/**
 * Inbound port for filtered, paginated, sorted transaction search (FR-08, UC-04, SAD 34.6-34.8,
 * TDS §31).
 */
public interface SearchTransactionsUseCase {

	PagedResult<TransactionSearchResultItem> search(TransactionSearchCriteria criteria);

}
