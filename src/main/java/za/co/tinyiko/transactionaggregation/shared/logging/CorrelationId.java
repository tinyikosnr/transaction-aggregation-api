package za.co.tinyiko.transactionaggregation.shared.logging;

import java.util.Objects;

/**
 * The identifier used to trace one request across controllers, services, logs, audit events
 * and error responses (SAD Part 5 34.10, Part 6A 37).
 *
 * <p>Held as an opaque, validated string rather than a {@code UUID} — the API accepts a
 * valid client-supplied correlation ID and only generates one (typically UUID-shaped) when
 * none is supplied, so this type must not force UUID formatting on values a client already
 * chose.
 */
public record CorrelationId(String value) {

	/** The request/response header this value is read from and written to. */
	public static final String HEADER_NAME = "X-Correlation-ID";

	public CorrelationId {
		Objects.requireNonNull(value, "value must not be null");
		if (value.isBlank()) {
			throw new IllegalArgumentException("value must not be blank");
		}
	}

}
