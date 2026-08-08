/**
 * Aggregation's public API: the four {@code Get*SummaryUseCase} inbound ports and their
 * {@code *SummaryView}/{@code CustomerNotFoundException} shapes.
 *
 * <p>Exposed at the package level, matching the same strategy already used by
 * {@code categorisation.application}, {@code customer.application}, {@code merchant.application}
 * and {@code audit.application} - one {@code @NamedInterface} on the package rather than
 * annotating each exported type individually. Added in {@code feature/api}: {@code api} is this
 * package's first genuine external caller, so a bare module dependency on {@code aggregation}
 * would only reach its (empty) default interface, not this named one - the same
 * {@code "module :: application"} qualified-dependency lesson already learned in
 * {@code transaction}'s own package-info.
 */
@org.springframework.modulith.NamedInterface
package za.co.tinyiko.transactionaggregation.aggregation.application;
