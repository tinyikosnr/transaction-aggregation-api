package za.co.tinyiko.transactionaggregation.shared.logging;

import java.util.Objects;
import java.util.UUID;

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

	/**
	 * The {@code HttpServletRequest} attribute {@code config.CorrelationIdFilter} stores the
	 * resolved {@link CorrelationId} under, for {@code api.controller} to read back - kept here,
	 * on the value object itself, rather than as a constant on the filter class, so both
	 * {@code config} and {@code api} can share it via {@code shared} (already {@code OPEN})
	 * without {@code api} needing a new module dependency on {@code config} just to reach it.
	 */
	public static final String REQUEST_ATTRIBUTE_NAME = "correlationId";

	public CorrelationId {
		Objects.requireNonNull(value, "value must not be null");
		if (value.isBlank()) {
			throw new IllegalArgumentException("value must not be blank");
		}
	}

	/**
	 * Generates a fresh correlation id, used when a request arrives with no
	 * {@value #HEADER_NAME} header. Added in {@code feature/api} for
	 * {@code config.CorrelationIdFilter} - the first real caller that needs to generate one
	 * rather than construct it from an already-known value.
	 */
	public static CorrelationId generate() {
		return new CorrelationId(UUID.randomUUID().toString());
	}

}
