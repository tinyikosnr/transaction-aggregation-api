/**
 * Merchant's public API: {@link za.co.tinyiko.transactionaggregation.merchant.application.MerchantResolutionPort}
 * and its {@code MerchantResolutionResult} shape.
 *
 * <p>Exposed at the package level, per the pattern established for categorisation and audit -
 * one {@code @NamedInterface} on the package, rather than annotating each exported type
 * individually. {@link MerchantResolutionService} stays internal regardless (it's
 * package-private, so it's never importable cross-module either way).
 */
@org.springframework.modulith.NamedInterface
package za.co.tinyiko.transactionaggregation.merchant.application;
