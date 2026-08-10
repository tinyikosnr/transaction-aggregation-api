package za.co.tinyiko.transactionaggregation.transaction.port;

/**
 * The closed vocabulary for {@link TransactionMetricsPort#transactionRejected}'s {@code reason}
 * tag (feature/observability, SAD 38.3: "Metric labels must have controlled cardinality"). Kept
 * as a compile-time-bounded enum, not a raw {@code String}, so the tag's value set can never grow
 * unbounded by accident - deliberately does not distinguish duplicate rejections (see
 * {@link TransactionMetricsPort#transactionDuplicateRejected()}, its own separate counter) and
 * never carries an exception message.
 */
public enum RejectionReason {

	VALIDATION_FAILED,
	SOURCE_NOT_FOUND,
	CUSTOMER_NOT_FOUND

}
