package za.co.tinyiko.transactionaggregation.categorisation.application;

/**
 * Thrown by {@link CategorisationRuleAdminService} when a create/update command fails a
 * structural or business invariant (unknown {@code matchField}/{@code operator}/{@code direction}
 * value, blank/too-long {@code matchValue}, non-positive {@code priority}, invalid {@code REGEX}
 * pattern) - the same translation role {@code transaction.application.TransactionValidationException}
 * already plays for single-create, reusing the existing, already-catalogued
 * {@code REQUEST_VALIDATION_FAILED} code rather than introducing a new one.
 */
public class RuleValidationException extends RuntimeException {

	public RuleValidationException(String message, Throwable cause) {
		super(message, cause);
	}

}
