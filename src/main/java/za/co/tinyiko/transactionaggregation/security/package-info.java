/**
 * Cross-cutting security concerns: JWT validation, authority/role extraction and
 * access-denied handling.
 *
 * <p>Not a data-owning business module. {@link za.co.tinyiko.transactionaggregation.security.SecurityConfig}
 * (public - imported by {@code config} to wire it, and by {@code api.controller} tests) is the
 * only public type here; its collaborators are package-private, constructed directly by it.
 *
 * <p>{@code allowedDependencies = "shared"}: the one real dependency (used by
 * {@code ProblemDetailSupport} for {@code CorrelationId}). No business module may depend on
 * {@code security}, in either direction - enforced by {@code architecture.SecurityArchitectureTests}
 * in addition to Spring Modulith's own {@code verify()}.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = "shared")
package za.co.tinyiko.transactionaggregation.security;
