/**
 * Transaction module — owns {@code transactions} and {@code transaction_sources}.
 *
 * <p>Responsible for transaction ingestion, validation, duplicate detection, persistence and
 * search. Depends on the customer, merchant, categorisation and audit modules' application
 * interfaces. No other module may access this module's {@code persistence} package directly.
 */
package za.co.tinyiko.transactionaggregation.transaction;
