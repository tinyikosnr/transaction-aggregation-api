package za.co.tinyiko.transactionaggregation.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import tools.jackson.databind.ObjectMapper;

/**
 * Produces an RFC 9457 {@link org.springframework.http.ProblemDetail} (403, {@code ACCESS_DENIED})
 * for every authenticated-but-forbidden request - both a {@code requestMatchers} rejection and an
 * {@code @PreAuthorize} denial reach here via Spring Security's {@code ExceptionTranslationFilter},
 * which wraps the entire downstream chain including the {@code DispatcherServlet}'s invocation of
 * the controller method the method-security interceptor guards.
 */
class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	ProblemDetailAccessDeniedHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
			throws IOException, ServletException {
		ProblemDetailSupport.write(request, response, objectMapper, HttpStatus.FORBIDDEN, "ACCESS_DENIED",
				"You do not have permission to access this resource.");
	}

}
