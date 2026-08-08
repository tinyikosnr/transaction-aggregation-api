package za.co.tinyiko.transactionaggregation.config;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;

/**
 * Resolves the correlation id for every request (SAD 34.10, TDS 67): honours a valid
 * client-supplied {@value CorrelationId#HEADER_NAME}, generates one when absent, exposes it as a
 * request attribute for controllers to read (see {@link CorrelationId#REQUEST_ATTRIBUTE_NAME}),
 * returns it on the response header, and places it in the logging MDC for the duration of the request so every
 * log line emitted while handling the request carries it - removed in a {@code finally} block so
 * it never leaks onto a thread-pooled worker's next, unrelated request.
 *
 * <p>{@code MAX_HEADER_LENGTH} matches {@code audit_events.correlation_id}'s {@code VARCHAR(100)}
 * column exactly (see {@code V7__create_audit_events_table.sql}): a client-supplied value longer
 * than that would otherwise flow untouched through {@code CreateTransactionCommand} into
 * {@code RecordAuditEventCommand} and only fail once Postgres rejects the insert, deep inside an
 * otherwise-successful transaction-creation flow. Rejected here instead, at the boundary, before
 * it can become a persistence failure - {@code chain.doFilter} is never called for an oversized
 * value, so the request never reaches the application layer at all.
 *
 * <p>{@code @Order(HIGHEST_PRECEDENCE)} so the MDC value is set before any other filter (Spring
 * Security's placeholder chain included) does its own logging for this request.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationIdFilter extends OncePerRequestFilter {

	private static final int MAX_HEADER_LENGTH = 100;
	private static final String MDC_KEY = "correlationId";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String headerValue = request.getHeader(CorrelationId.HEADER_NAME);

		if (headerValue != null && headerValue.length() > MAX_HEADER_LENGTH) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"%s must not exceed %d characters".formatted(CorrelationId.HEADER_NAME, MAX_HEADER_LENGTH));
			return;
		}

		CorrelationId correlationId = (headerValue == null || headerValue.isBlank())
				? CorrelationId.generate()
				: new CorrelationId(headerValue);

		request.setAttribute(CorrelationId.REQUEST_ATTRIBUTE_NAME, correlationId);
		response.setHeader(CorrelationId.HEADER_NAME, correlationId.value());

		MDC.put(MDC_KEY, correlationId.value());
		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove(MDC_KEY);
		}
	}

}
