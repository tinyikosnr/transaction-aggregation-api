/**
 * Aggregation module — owns no persistent data.
 *
 * <p>Computes customer, category, merchant and monthly financial summaries by reading
 * transaction and customer data through read-only query ports. Must never write transaction
 * data and must never access another module's {@code persistence} package directly.
 */
package za.co.tinyiko.transactionaggregation.aggregation;
