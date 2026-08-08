package za.co.tinyiko.transactionaggregation.security;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * See {@link ProblemDetailAuthenticationEntryPointTests}'s Javadoc for why the {@link ObjectMapper}
 * here explicitly registers {@link ProblemDetailJacksonMixin}.
 */
class ProblemDetailAccessDeniedHandlerTests {

	private final ObjectMapper objectMapper = JsonMapper.builder()
			.addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class)
			.build();
	private final ProblemDetailAccessDeniedHandler accessDeniedHandler = new ProblemDetailAccessDeniedHandler(objectMapper);

	@Test
	void respondsWithAccessDenied() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		accessDeniedHandler.handle(request, response, new AccessDeniedException("denied"));

		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(response.getContentType()).isEqualTo("application/problem+json");
		Map<?, ?> body = objectMapper.readValue(response.getContentAsString(), Map.class);
		assertThat(body.get("status")).isEqualTo(403);
		assertThat(body.get("errorCode")).isEqualTo("ACCESS_DENIED");
	}

	@Test
	void includesTheCorrelationIdWhenPresentOnTheRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(CorrelationId.REQUEST_ATTRIBUTE_NAME, new CorrelationId("access-denied-test-id"));
		MockHttpServletResponse response = new MockHttpServletResponse();

		accessDeniedHandler.handle(request, response, new AccessDeniedException("denied"));

		Map<?, ?> body = objectMapper.readValue(response.getContentAsString(), Map.class);
		assertThat(body.get("correlationId")).isEqualTo("access-denied-test-id");
	}

	@Test
	void omitsCorrelationIdWhenAbsentFromTheRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		accessDeniedHandler.handle(request, response, new AccessDeniedException("denied"));

		Map<String, Object> body = objectMapper.readValue(response.getContentAsString(), Map.class);
		assertThat(body).doesNotContainKey("correlationId");
	}

}
