/**
 * Audit's public API: {@link za.co.tinyiko.transactionaggregation.audit.application.RecordAuditEventUseCase}
 * and its {@code RecordAuditEventCommand} shape.
 *
 * <p>Exposed at the package level, per the pattern established for categorisation - one
 * {@code @NamedInterface} on the package, rather than annotating each exported type
 * individually. {@link AuditService} stays internal regardless (it's package-private, so it's
 * never importable cross-module either way).
 */
@org.springframework.modulith.NamedInterface
package za.co.tinyiko.transactionaggregation.audit.application;
