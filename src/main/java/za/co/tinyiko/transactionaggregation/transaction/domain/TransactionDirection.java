package za.co.tinyiko.transactionaggregation.transaction.domain;

/**
 * Whether a transaction is money coming in or going out (SAD 27.2, 28.7). Independent of
 * {@code categorisation.domain.Direction} - the two enums share identical constant names and
 * are connected only by an explicit {@code name()} string crossing the module boundary (see
 * {@code CreateTransactionService}), not a shared type. Promoting either to {@code shared} was
 * considered and rejected: see CLAUDE.md's "sharing a concept across two modules is not, by
 * itself, a reason to promote it" rule.
 */
public enum TransactionDirection {

	CREDIT,
	DEBIT

}
