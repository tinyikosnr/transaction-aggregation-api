package za.co.tinyiko.transactionaggregation.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import tools.jackson.databind.ObjectMapper;

/**
 * Produces an RFC 9457 {@link org.springframework.http.ProblemDetail} for every 401, matching
 * SAD 39.4's two distinct codes (not TDS 40's single, coarser {@code SEC-001}): a bearer token
 * that was supplied but failed validation gets {@code TOKEN_INVALID}; no credentials at all gets
 * {@code AUTHENTICATION_REQUIRED}. Distinguished via Spring Security's own standard exception
 * shape - an {@link OAuth2AuthenticationException} carrying a {@code BearerTokenError} with
 * {@code errorCode = "invalid_token"} is what Spring's resource-server support raises for a
 * malformed/expired/bad-signature token - not custom token inspection.
 */
class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	ProblemDetailAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
			throws IOException, ServletException {
		if (isInvalidToken(authException)) {
			ProblemDetailSupport.write(request, response, objectMapper, HttpStatus.UNAUTHORIZED, "TOKEN_INVALID",
					"The supplied bearer token is invalid, malformed, or expired.");
		} else {
			ProblemDetailSupport.write(request, response, objectMapper, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED",
					"Authentication is required to access this resource.");
		}
	}

	private static boolean isInvalidToken(AuthenticationException authException) {
		if (!(authException instanceof OAuth2AuthenticationException oAuth2AuthenticationException)) {
			return false;
		}
		OAuth2Error error = oAuth2AuthenticationException.getError();
		return error != null && "invalid_token".equals(error.getErrorCode());
	}

}
