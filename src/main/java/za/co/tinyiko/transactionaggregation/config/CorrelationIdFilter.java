package za.co.tinyiko.transactionaggregation.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

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
 *
 * <p><strong>Access logging (feature/structured-logging, SAD 37.1/37.3's "Correlation and Access
 * Log Filter"):</strong> one {@code INFO} event is logged after {@code filterChain.doFilter}
 * returns, carrying only {@code http.request.method}/{@code http.response.status_code}/
 * {@code event.duration} (elapsed {@link System#nanoTime()}, never wall-clock, matching ECS's
 * documented unit for this field) and, when available, {@code http.route}. {@code correlationId}
 * reaches this event automatically through MDC - Spring Boot 4.1's structured JSON formatters
 * include MDC/context data by default (empirically verified: {@code correlationId} appeared in a
 * real captured log line with no {@code logging.structured.json.context.include} property set at
 * all, so none is configured). {@code http.route} is read from
 * {@link HandlerMapping#BEST_MATCHING_PATTERN_ATTRIBUTE} - the matched Spring MVC route
 * <em>template</em> (e.g. {@code /api/v1/transactions/{id}}), never {@link
 * HttpServletRequest#getRequestURI()} (the resolved path, which can carry a real transaction/
 * customer UUID) and never a query string. Empirically verified against a real running instance
 * (temporary in-place edit, reverted before commit): a routed request to
 * {@code /api/v1/transactions/<uuid>} produced {@code http.route=/api/v1/transactions/{id}}, and a
 * request rejected by Spring Security before any handler was resolved produced no
 * {@code http.route} at all - the attribute is simply absent in that case, so the field is
 * omitted rather than falling back to the raw URI. No headers, cookies, query string, or request/
 * response body are ever read here.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationIdFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);
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

		long startNanos = System.nanoTime();
		MDC.put(MDC_KEY, correlationId.value());
		try {
			filterChain.doFilter(request, response);
		} finally {
			logAccess(request, response, System.nanoTime() - startNanos);
			MDC.remove(MDC_KEY);
		}
	}

	private void logAccess(HttpServletRequest request, HttpServletResponse response, long elapsedNanos) {
		var event = log.atInfo()
				.addKeyValue("http.request.method", request.getMethod())
				.addKeyValue("http.response.status_code", response.getStatus())
				.addKeyValue("event.duration", elapsedNanos);

		Object matchedRoute = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		if (matchedRoute != null) {
			event = event.addKeyValue("http.route", matchedRoute.toString());
		}

		event.log("Request completed");
	}

}
