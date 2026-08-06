package za.co.tinyiko.transactionaggregation.audit.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * The identity of an {@link AuditEvent}, per TDS 59. Same pattern as every other id type in
 * this codebase: generated here, via {@link #generate()}, not inside the aggregate's factory
 * method.
 */
public record AuditEventId(UUID value) {

	public AuditEventId {
		Objects.requireNonNull(value, "value must not be null");
	}

	public static AuditEventId generate() {
		return new AuditEventId(UUID.randomUUID());
	}

}
