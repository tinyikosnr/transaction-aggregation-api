package za.co.tinyiko.transactionaggregation.transaction.port;

/**
 * Outbound port for the small, SAD-38.3-literal set of transaction business metrics
 * (feature/observability) - a strongly-typed, narrow-operation port, deliberately not a generic
 * {@code record(String metricName, Map<String,String> tags)} API: a bounded set of named methods
 * is the mechanism that keeps every tag value compile-time-bounded (see {@link RejectionReason}),
 * rather than trusting every future call site to remember the cardinality rules by convention.
 *
 * <p>No Micrometer type is referenced anywhere in this interface - {@code MicrometerTransactionMetrics}
 * (the sole implementation, in {@code transaction.metrics}) is the only place in this codebase
 * that imports {@code io.micrometer.*} for this concern, verified by
 * {@code TransactionArchitectureTests}.
 *
 * <p>Every method here is a best-effort, in-process operational signal, not a transactionally-
 * guaranteed post-commit record - see {@link ProcessingTimer} and
 * {@code CreateTransactionService}'s own Javadoc for the precise semantics of when each is called
 * relative to the surrounding {@code @Transactional} boundary.
 */
public interface TransactionMetricsPort {

	/**
	 * Every ingestion attempt, single or bulk-item, before any validation - the one signal
	 * {@code http.server.requests} cannot give for bulk (one HTTP request, N items).
	 */
	void transactionReceived();

	/**
	 * The transaction-creation workflow reached its completed normal success path, including
	 * successful success-audit recording - not a guarantee that the enclosing database
	 * transaction has since physically committed (see {@code CreateTransactionService}).
	 */
	void transactionProcessed();

	/**
	 * One of the three non-duplicate rejection reasons was reached (validation, source not found,
	 * customer not found).
	 */
	void transactionRejected(RejectionReason reason);

	/**
	 * The transaction was rejected as a duplicate (BR-08) - a separate counter from
	 * {@link #transactionRejected}, matching SAD 38.3's own separate {@code transactions.duplicates}
	 * example.
	 */
	void transactionDuplicateRejected();

	/**
	 * A transaction that reached the completed normal success path and whose assigned category
	 * was the fallback category ({@code categorisation.application.CategorisationDecision#fallbackApplied()}).
	 */
	void categorisationFallback();

	/**
	 * Starts timing one transaction-processing attempt. The returned handle must be stopped
	 * exactly once, in a {@code finally} block, regardless of outcome - success, an expected
	 * rejection, a duplicate, an audit-recording failure, or an unexpected systemic exception all
	 * count toward {@code transactions.processing.duration}.
	 */
	ProcessingTimer startProcessingTimer();

	/**
	 * A framework-independent handle for one in-flight timing measurement. No Micrometer
	 * {@code Timer.Sample} (or any other Micrometer type) is exposed through this interface -
	 * {@code MicrometerTransactionMetrics} owns the sample internally and closes over it here.
	 */
	interface ProcessingTimer {
		void stop();
	}

}
