/**
 * Reusable, business-rule-free technical building blocks available to every module
 * (exceptions, logging support, validation helpers, the domain-event envelope, generic
 * utilities).
 *
 * <p>{@code shared} must never contain domain-specific business logic and must never depend
 * on a business module — enforced below via {@code allowedDependencies = {}}, since this is
 * one rule Spring Modulith's default package-based detection cannot express on its own. It is
 * declared {@code OPEN} so every other module may depend on it freely, including any module
 * that later declares its own {@code allowedDependencies} restriction.
 */
@ApplicationModule(
		type = ApplicationModule.Type.OPEN,
		allowedDependencies = {}
)
package za.co.tinyiko.transactionaggregation.shared;

import org.springframework.modulith.ApplicationModule;
