package za.co.tinyiko.transactionaggregation.customer.domain;

/**
 * Customer lifecycle status (SAD §27.1). A suspended or inactive customer cannot receive new
 * transactions unless explicitly permitted by a future business rule — that rule is enforced
 * by the transaction module when it exists, not here; this module only holds and transitions
 * the status.
 */
public enum CustomerStatus {

	ACTIVE,
	INACTIVE,
	SUSPENDED

}
