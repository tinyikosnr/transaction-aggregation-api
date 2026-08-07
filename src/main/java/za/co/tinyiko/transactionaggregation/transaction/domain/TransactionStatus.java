package za.co.tinyiko.transactionaggregation.transaction.domain;

/**
 * Transaction processing status (SAD 27.2, 28.7, and the recommended {@code CHECK} constraint
 * at SAD 32.4). Follows SAD over TDS: SAD lists exactly these three values; TDS's five-value
 * version ({@code VALIDATED}, {@code DUPLICATE} in addition) is not followed.
 *
 * <p>In this branch's synchronous, single-transaction ingestion flow, {@link Transaction} is
 * only ever constructed (via {@link Transaction#register}) with status {@code PROCESSED} - a
 * validation failure, duplicate, unsupported source or missing customer is rejected before a
 * {@code Transaction} is ever created, via an exception, not by persisting a row with status
 * {@code RECEIVED} or {@code REJECTED}. Those two values remain part of the enum and the
 * database {@code CHECK} constraint for schema completeness and forward compatibility (e.g. a
 * future asynchronous ingestion pipeline), not because current code paths produce them.
 */
public enum TransactionStatus {

	RECEIVED,
	PROCESSED,
	REJECTED

}
