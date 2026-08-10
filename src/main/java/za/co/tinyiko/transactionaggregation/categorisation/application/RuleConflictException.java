package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.util.UUID;

/**
 * Thrown by {@code JpaCategorisationRuleRepositoryAdapter#update} when a rule update's
 * {@code expectedVersion} no longer matches the persisted row - either caught by the adapter's
 * own explicit pre-flush comparison (the common case: the caller's data was already stale before
 * the write was attempted) or by Hibernate's {@code ObjectOptimisticLockingFailureException} at
 * flush time (a genuinely concurrent write landing in the narrow window between the adapter's own
 * load and its flush - see that adapter's own Javadoc for exactly why both paths are safe).
 * Mapped to the existing, previously-unused SAD 39.4 {@code OPTIMISTIC_LOCK_CONFLICT} (409) code -
 * no new error code needed here, unlike {@link RuleNotFoundException}.
 */
public class RuleConflictException extends RuntimeException {

	public RuleConflictException(UUID ruleId, long expectedVersion, long actualVersion) {
		super("Categorisation rule " + ruleId + " was modified concurrently: expected version "
				+ expectedVersion + " but was " + actualVersion);
	}

	public RuleConflictException(UUID ruleId, Throwable cause) {
		super("Categorisation rule " + ruleId + " was modified concurrently", cause);
	}

}
