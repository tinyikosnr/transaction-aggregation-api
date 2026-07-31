/**
 * Categorisation's public API: {@link za.co.tinyiko.transactionaggregation.categorisation.application.CategoriseTransactionUseCase}
 * and its {@code CategorisationInput}/{@code CategorisationDecision} shapes.
 *
 * <p>Exposed at the package level, per the agreed strategy for consuming a module's API from
 * outside it - one {@code @NamedInterface} on the package, rather than annotating each exported
 * type individually. {@link CategorisationService} stays internal regardless (it's
 * package-private, so it's never importable cross-module either way).
 */
@org.springframework.modulith.NamedInterface
package za.co.tinyiko.transactionaggregation.categorisation.application;
