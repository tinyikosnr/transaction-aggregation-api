/**
 * Audit module — owns {@code audit_events}.
 *
 * <p>Records an append-only trail of business-significant events for traceability. Audit
 * events are never updated or deleted. Has no outbound dependency on any other business
 * module.
 */
package za.co.tinyiko.transactionaggregation.audit;
