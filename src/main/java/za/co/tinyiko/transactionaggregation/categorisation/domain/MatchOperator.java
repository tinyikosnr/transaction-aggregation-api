package za.co.tinyiko.transactionaggregation.categorisation.domain;

/**
 * How a {@link CategorisationRule}'s {@code matchValue} is compared against the selected field
 * (SAD 27.6). The seed data (V6 migration) exercises {@code CONTAINS} for keyword rules and
 * {@code REGEX} for the two direction-wide catch-all rules; {@code EQUALS} is modelled because
 * SAD documents it, though nothing seeds one yet.
 */
public enum MatchOperator {

	EQUALS,
	CONTAINS,
	REGEX

}
