package za.co.tinyiko.transactionaggregation.security;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@link ObjectMapper} used here registers {@link ProblemDetailJacksonMixin} explicitly -
 * the same mixin Spring Boot's Jackson autoconfiguration applies to the app's real, injected
 * {@code ObjectMapper} bean (proven working end-to-end by {@code TransactionControllerTests}'s
 * {@code jsonPath} assertions) - a bare {@code new ObjectMapper()} would serialize
 * {@code ProblemDetail.getProperties()} as a nested object instead of flattening it to top-level
 * fields, which is not what the real bean does.
 */
class ProblemDetailAuthenticationEntryPointTests {

	private final ObjectMapper objectMapper = JsonMapper.builder()
			.addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class)
			.build();
	private final ProblemDetailAuthenticationEntryPoint entryPoint = new ProblemDetailAuthenticationEntryPoint(objectMapper);

	@Test
	void respondsWithAuthenticationRequiredWhenNoCredentialsWereSupplied() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		entryPoint.commence(request, response, new InsufficientAuthenticationException("Full authentication is required"));

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentType()).isEqualTo("application/problem+json");
		Map<?, ?> body = objectMapper.readValue(response.getContentAsString(), Map.class);
		assertThat(body.get("status")).isEqualTo(401);
		assertThat(body.get("errorCode")).isEqualTo("AUTHENTICATION_REQUIRED");
	}

	@Test
	void respondsWithTokenInvalidWhenTheTokenFailedValidation() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		OAuth2AuthenticationException exception = new OAuth2AuthenticationException(new OAuth2Error("invalid_token"));

		entryPoint.commence(request, response, exception);

		assertThat(response.getStatus()).isEqualTo(401);
		Map<?, ?> body = objectMapper.readValue(response.getContentAsString(), Map.class);
		assertThat(body.get("errorCode")).isEqualTo("TOKEN_INVALID");
	}

	@Test
	void includesTheCorrelationIdWhenPresentOnTheRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(CorrelationId.REQUEST_ATTRIBUTE_NAME, new CorrelationId("entry-point-test-id"));
		MockHttpServletResponse response = new MockHttpServletResponse();

		entryPoint.commence(request, response, new InsufficientAuthenticationException("no credentials"));

		Map<?, ?> body = objectMapper.readValue(response.getContentAsString(), Map.class);
		assertThat(body.get("correlationId")).isEqualTo("entry-point-test-id");
	}

	@Test
	void omitsCorrelationIdWhenAbsentFromTheRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		entryPoint.commence(request, response, new InsufficientAuthenticationException("no credentials"));

		Map<String, Object> body = objectMapper.readValue(response.getContentAsString(), Map.class);
		assertThat(body).doesNotContainKey("correlationId");
	}

}
