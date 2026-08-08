package za.co.tinyiko.transactionaggregation.config;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

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

}
