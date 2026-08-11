package za.co.tinyiko.transactionaggregation.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.KeyValuePair;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTests {

	private final CorrelationIdFilter filter = new CorrelationIdFilter();

	@Test
	void honoursAValidClientSuppliedHeader() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(CorrelationId.HEADER_NAME, "client-supplied-id");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getHeader(CorrelationId.HEADER_NAME)).isEqualTo("client-supplied-id");
		assertThat(request.getAttribute(CorrelationId.REQUEST_ATTRIBUTE_NAME)).isEqualTo(new CorrelationId("client-supplied-id"));
	}

	@Test
	void generatesAValueWhenHeaderIsAbsent() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getHeader(CorrelationId.HEADER_NAME)).isNotBlank();
		assertThat(request.getAttribute(CorrelationId.REQUEST_ATTRIBUTE_NAME)).isInstanceOf(CorrelationId.class);
	}

	@Test
	void generatesAValueWhenHeaderIsBlank() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(CorrelationId.HEADER_NAME, "   ");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getHeader(CorrelationId.HEADER_NAME)).isNotBlank();
	}

	@Test
	void rejectsAnOversizedHeaderWithBadRequestAndNeverContinuesTheChain() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(CorrelationId.HEADER_NAME, "x".repeat(101));
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(400);
		assertThat(chain.getRequest()).isNull();
	}

	@Test
	void acceptsAHeaderAtExactlyTheMaximumLength() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(CorrelationId.HEADER_NAME, "x".repeat(100));
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getHeader(CorrelationId.HEADER_NAME)).isEqualTo("x".repeat(100));
	}

	@Test
	void putsTheCorrelationIdInMdcForTheDurationOfTheRequestAndRemovesItAfterwards() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(CorrelationId.HEADER_NAME, "mdc-test-id");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = (req, res) -> assertThat(MDC.get("correlationId")).isEqualTo("mdc-test-id");

		filter.doFilter(request, response, chain);

		assertThat(MDC.get("correlationId")).isNull();
	}

	@Test
	void removesMdcValueEvenWhenTheChainThrows() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(CorrelationId.HEADER_NAME, "mdc-error-id");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = (req, res) -> {
			throw new IllegalStateException("downstream failure");
		};

		try {
			filter.doFilter(request, response, chain);
		} catch (Exception ignored) {
			// expected - asserting cleanup happens regardless
		}

		assertThat(MDC.get("correlationId")).isNull();
	}

	/**
	 * Servlet threads are reused across unrelated requests - proves a second request that supplies
	 * no header never observes the first request's correlation id, either via MDC or via its own
	 * generated value, closing the specific cross-request leakage scenario the per-request cleanup
	 * tests above don't individually cover.
	 */
	@Test
	void secondRequestWithoutAHeaderDoesNotInheritTheFirstRequestsCorrelationId() throws ServletException, IOException {
		MockHttpServletRequest requestA = new MockHttpServletRequest();
		requestA.addHeader(CorrelationId.HEADER_NAME, "request-a-id");
		MockHttpServletResponse responseA = new MockHttpServletResponse();

		filter.doFilter(requestA, responseA, new MockFilterChain());

		assertThat(responseA.getHeader(CorrelationId.HEADER_NAME)).isEqualTo("request-a-id");
		assertThat(MDC.get("correlationId")).isNull();

		MockHttpServletRequest requestB = new MockHttpServletRequest();
		MockHttpServletResponse responseB = new MockHttpServletResponse();

		filter.doFilter(requestB, responseB, new MockFilterChain());

		String generatedForB = responseB.getHeader(CorrelationId.HEADER_NAME);
		assertThat(generatedForB).isNotBlank().isNotEqualTo("request-a-id");
		assertThat(MDC.get("correlationId")).isNull();
	}

	@Test
	void logsOneAccessEventWithMethodStatusAndDuration() throws ServletException, IOException {
		ListAppender<ILoggingEvent> appender = attachTestAppender();
		try {
			MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/transactions");
			MockHttpServletResponse response = new MockHttpServletResponse();
			FilterChain chain = (req, res) -> ((MockHttpServletResponse) res).setStatus(200);

			filter.doFilter(request, response, chain);

			assertThat(appender.list).hasSize(1);
			ILoggingEvent event = appender.list.get(0);
			assertThat(event.getLevel()).isEqualTo(Level.INFO);
			assertThat(event.getFormattedMessage()).isEqualTo("Request completed");

			Map<String, Object> fields = toMap(event.getKeyValuePairs());
			assertThat(fields.get("http.request.method")).isEqualTo("GET");
			assertThat(fields.get("http.response.status_code")).isEqualTo(200);
			assertThat(fields.get("event.duration")).isInstanceOf(Long.class);
			assertThat((Long) fields.get("event.duration")).isGreaterThanOrEqualTo(0L);
		} finally {
			detachTestAppender(appender);
		}
	}

	@Test
	void includesHttpRouteWhenAMatchedMvcPatternIsPresentOnTheRequest() throws ServletException, IOException {
		ListAppender<ILoggingEvent> appender = attachTestAppender();
		try {
			MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/transactions/11111111-1111-1111-1111-111111111111");
			MockHttpServletResponse response = new MockHttpServletResponse();
			FilterChain chain = (req, res) -> {
				req.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/v1/transactions/{id}");
				((MockHttpServletResponse) res).setStatus(200);
			};

			filter.doFilter(request, response, chain);

			Map<String, Object> fields = toMap(appender.list.get(0).getKeyValuePairs());
			assertThat(fields.get("http.route")).isEqualTo("/api/v1/transactions/{id}");
			assertThat(fields.values()).noneMatch(value -> value instanceof String s && s.contains("11111111-1111-1111-1111-111111111111"));
		} finally {
			detachTestAppender(appender);
		}
	}

	/**
	 * A request rejected before any Spring MVC handler is resolved (e.g. by Spring Security)
	 * never has {@link HandlerMapping#BEST_MATCHING_PATTERN_ATTRIBUTE} set - {@code http.route}
	 * must be omitted entirely in that case, never falling back to the raw request URI.
	 */
	@Test
	void omitsHttpRouteWhenNoMatchedMvcPatternExists() throws ServletException, IOException {
		ListAppender<ILoggingEvent> appender = attachTestAppender();
		try {
			MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/transactions/11111111-1111-1111-1111-111111111111");
			MockHttpServletResponse response = new MockHttpServletResponse();
			FilterChain chain = (req, res) -> ((MockHttpServletResponse) res).setStatus(401);

			filter.doFilter(request, response, chain);

			Map<String, Object> fields = toMap(appender.list.get(0).getKeyValuePairs());
			assertThat(fields).doesNotContainKey("http.route");
		} finally {
			detachTestAppender(appender);
		}
	}

	private static ListAppender<ILoggingEvent> attachTestAppender() {
		Logger logbackLogger = (Logger) LoggerFactory.getLogger(CorrelationIdFilter.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logbackLogger.addAppender(appender);
		return appender;
	}

	private static void detachTestAppender(ListAppender<ILoggingEvent> appender) {
		Logger logbackLogger = (Logger) LoggerFactory.getLogger(CorrelationIdFilter.class);
		logbackLogger.detachAppender(appender);
		appender.stop();
	}

	private static Map<String, Object> toMap(List<KeyValuePair> pairs) {
		Map<String, Object> map = new HashMap<>();
		pairs.forEach(pair -> map.put(pair.key, pair.value));
		return map;
	}

}
