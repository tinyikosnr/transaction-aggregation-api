/**
 * Customer's public API: {@link za.co.tinyiko.transactionaggregation.customer.application.CustomerExistsPort}
 * (the only inbound port used cross-module so far) and {@code CustomerLookupPort}.
 *
 * <p>Exposed at the package level, per the pattern established for categorisation and audit -
 * one {@code @NamedInterface} on the package, rather than annotating each exported type
 * individually. {@link CustomerLookupService}, {@link CustomerRegistrationService} stay
 * internal regardless (both package-private, so never importable cross-module either way).
 */
@org.springframework.modulith.NamedInterface
package za.co.tinyiko.transactionaggregation.customer.application;
