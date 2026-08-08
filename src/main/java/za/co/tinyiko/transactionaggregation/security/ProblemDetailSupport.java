package za.co.tinyiko.transactionaggregation.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import tools.jackson.databind.ObjectMapper;

import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;

/**
 * Shared RFC 9457 {@link ProblemDetail} response-writing logic for {@link ProblemDetailAuthenticationEntryPoint}
 * and {@link ProblemDetailAccessDeniedHandler} - both run inside Spring Security's
 * {@code ExceptionTranslationFilter}, outside the {@code DispatcherServlet}/MVC message-converter
 * pipeline entirely, so unlike {@code api.advice.GlobalExceptionHandler} they cannot simply return
 * a {@code ResponseEntity<ProblemDetail>} and let Spring MVC render it - the response must be
 * written directly. Kept local to {@code security} rather than shared with
 * {@code api.advice.GlobalExceptionHandler}'s own near-identical helper: sharing it would require
 * a new, backwards {@code security -> api} (or {@code config -> api}) dependency for a handful of
 * duplicated lines, which is a worse trade than the small duplication.
 */
final class ProblemDetailSupport {

	private ProblemDetailSupport() {
	}

	static void write(
			HttpServletRequest request,
			HttpServletResponse response,
			ObjectMapper objectMapper,
			HttpStatus status,
			String errorCode,
			String detail
	) throws IOException {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
		problemDetail.setProperty("errorCode", errorCode);
		problemDetail.setProperty("timestamp", Instant.now());
		if (request.getAttribute(CorrelationId.REQUEST_ATTRIBUTE_NAME) instanceof CorrelationId correlationId) {
			problemDetail.setProperty("correlationId", correlationId.value());
		}

		response.setStatus(status.value());
		response.setContentType("application/problem+json");
		response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
	}

}
