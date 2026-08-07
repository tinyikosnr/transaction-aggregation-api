/**
 * Aggregation module - owns no persistent data.
 *
 * <p>Computes customer, category, merchant and monthly financial summaries by reading
 * transaction and customer data through read-only query ports. Must never write transaction
 * data and must never access another module's {@code persistence} package directly.
 *
 * <p>{@code allowedDependencies} lists only what SAD's dependency diagram and TDS 6's own
 * "Dependencies" section document ("Transaction query port", "Customer module") - no
 * {@code merchant}, {@code categorisation}, {@code audit} or {@code shared}, none of which this
 * module has a genuine current need for. Both entries use the qualified {@code "module ::
 * application"} syntax (see {@code transaction}'s own package-info for why a bare module name
 * isn't equivalent).
 */
@org.springframework.modulith.ApplicationModule(
		allowedDependencies = {"transaction :: application", "customer :: application"})
package za.co.tinyiko.transactionaggregation.aggregation;
