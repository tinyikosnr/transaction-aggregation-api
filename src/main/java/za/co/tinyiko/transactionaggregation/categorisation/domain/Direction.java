package za.co.tinyiko.transactionaggregation.categorisation.domain;

/**
 * Whether a transaction is money coming in or going out (BR-12/13). Local to this module, not
 * shared: a second module (transaction, currently paused) will very likely need the identical
 * concept, but that alone isn't reason enough to promote it to {@code shared} now. Promotion is
 * a deliberate refactor to perform once a second real consumer exists and its usage is confirmed
 * semantically identical, not an upfront design choice made in anticipation of one.
 */
public enum Direction {

	CREDIT,
	DEBIT

}
