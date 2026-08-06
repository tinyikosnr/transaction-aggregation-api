/**
 * Audit module - owns {@code audit_events}.
 *
 * <p>Records an append-only trail of business-significant events for traceability. Audit
 * events are never updated or deleted. Depends on nothing but {@code shared} - enforced below,
 * since with another module ({@code transaction}) genuinely expected to depend on it, "audit
 * depends on nothing else" is now a real, verifiable rule rather than an incidental fact.
 * Unlike {@code categorisation}'s identical-looking declaration, this one is exercised by real
 * code: {@code AuditEvent}/{@code RecordAuditEventCommand} both hold a
 * {@code shared.logging.CorrelationId}, so {@code shared} must be explicitly listed here rather
 * than left as an empty allow-list - Spring Modulith's {@code type = OPEN} on {@code shared}
 * only means other modules don't need shared's permission to depend on it, not that it's
 * automatically exempt from a module's own {@code allowedDependencies} whitelist.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = "shared")
package za.co.tinyiko.transactionaggregation.audit;
