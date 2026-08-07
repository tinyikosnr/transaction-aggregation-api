package za.co.tinyiko.transactionaggregation.aggregation.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A reporting period (TDS 20). Both bounds inclusive - see {@code transaction.application.TransactionQueryService}
 * for how this translates to the UTC instant range actually queried.
 *
 * <p>Self-validating, matching every other value object in this codebase (e.g.
 * {@code transaction.domain.Money}): {@code from}/{@code to} non-null, {@code from} not after
 * {@code to}, and the span not exceeding 24 months (TDS 20: "Maximum query range for synchronous
 * API calls: 24 months"). Translating this {@code IllegalArgumentException} into a named,
 * catchable exception (TDS 40's {@code AGG-001}) is a {@code feature/api} concern, not this
 * branch's - nothing in {@code aggregation.application} constructs a {@code DateRange} from raw
 * input in this branch, callers are always handed an already-valid one.
 */
public record DateRange(LocalDate from, LocalDate to) {

	private static final int MAX_RANGE_MONTHS = 24;

	public DateRange {
		Objects.requireNonNull(from, "from must not be null");
		Objects.requireNonNull(to, "to must not be null");
		if (from.isAfter(to)) {
			throw new IllegalArgumentException("from must not be after to");
		}
		if (to.isAfter(from.plusMonths(MAX_RANGE_MONTHS))) {
			throw new IllegalArgumentException("date range must not exceed " + MAX_RANGE_MONTHS + " months");
		}
	}

}
