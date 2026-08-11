package za.co.tinyiko.transactionaggregation;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.UUID;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.read.ListAppender;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the structured-logging pipeline (feature/structured-logging) end-to-end against a real
 * request through the real filter chain, not just the unit-level {@code CorrelationIdFilterTests}/
 * {@code GlobalExceptionHandlerTests}. Rather than swapping {@link System#setOut} (Logback's
 * {@code ConsoleAppender} captures its target stream reference at start-up, before a test could
 * ever redirect it, so a naive stdout swap would silently capture nothing), this attaches a
 * {@link ListAppender} to the root logger to capture the real {@link ILoggingEvent}, then encodes
 * it through the exact same {@code CONSOLE} appender's {@link Encoder} Spring Boot itself
 * configured from {@code logging.structured.format.console=ecs} - proving the genuine, real ECS
 * JSON bytes that would have reached stdout, without any stream-redirection timing risk.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class StructuredLoggingEndToEndTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void accessLogForARoutedRequestIsValidEcsJsonWithTheExpectedFields() throws Exception {
		String correlationId = "structured-e2e-" + UUID.randomUUID();

		JsonNode event = captureAccessLogEvent(correlationId, () ->
				mockMvc.perform(get("/actuator/health").header(CorrelationId.HEADER_NAME, correlationId))
						.andExpect(status().isOk()));

		assertThat(event.get("@timestamp")).isNotNull();
		assertThat(event.get("ecs").get("version")).isNotNull();
		assertThat(event.get("log").get("level").asString()).isEqualTo("INFO");
		assertThat(event.get("log").get("logger").asString())
				.isEqualTo("za.co.tinyiko.transactionaggregation.config.CorrelationIdFilter");
		assertThat(event.get("message").asString()).isEqualTo("Request completed");
		assertThat(event.get("service").get("name").asString()).isEqualTo("transaction-aggregation-api");
		assertThat(event.get("correlationId").asString()).isEqualTo(correlationId);
		assertThat(event.get("http").get("request").get("method").asString()).isEqualTo("GET");
		assertThat(event.get("http").get("response").get("status_code").asInt()).isEqualTo(200);
		assertThat(event.get("http").get("route").asString()).isEqualTo("/actuator/health");
		assertThat(event.get("event").get("duration").asLong()).isGreaterThanOrEqualTo(0L);
	}

	@Test
	void generatedCorrelationIdMatchesTheResponseHeaderWhenNoHeaderIsSupplied() throws Exception {
		String[] generatedId = new String[1];

		JsonNode event = captureAccessLogEventMatching(
				candidate -> candidate.get("http") != null
						&& "/actuator/health".equals(candidate.path("http").path("route").asString(null)),
				() -> {
					MvcResult result = mockMvc.perform(get("/actuator/health")).andExpect(status().isOk()).andReturn();
					generatedId[0] = result.getResponse().getHeader(CorrelationId.HEADER_NAME);
				});

		assertThat(generatedId[0]).isNotBlank();
		assertThat(event.get("correlationId").asString()).isEqualTo(generatedId[0]);
	}

	@Test
	void accessLogOmitsHttpRouteAndNeverLeaksTheRawIdWhenRequestIsRejectedBeforeDispatch() throws Exception {
		String correlationId = "structured-e2e-401-" + UUID.randomUUID();
		UUID transactionId = UUID.randomUUID();

		JsonNode event = captureAccessLogEvent(correlationId, () ->
				mockMvc.perform(get("/api/v1/transactions/" + transactionId).header(CorrelationId.HEADER_NAME, correlationId))
						.andExpect(status().isUnauthorized()));

		assertThat(event.get("http").has("route")).isFalse();
		assertThat(event.toString()).doesNotContain(transactionId.toString());
	}

	@Test
	void accessLogNeverContainsTheSuppliedBearerToken() throws Exception {
		String correlationId = "structured-e2e-token-" + UUID.randomUUID();
		String secretToken = "super-secret-jwt-value-should-never-appear-in-a-log";
		given(jwtDecoder.decode(secretToken)).willThrow(new BadJwtException("invalid token"));

		JsonNode event = captureAccessLogEvent(correlationId, () ->
				mockMvc.perform(get("/actuator/health")
						.header(CorrelationId.HEADER_NAME, correlationId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + secretToken)));

		assertThat(event.toString()).doesNotContain(secretToken);
	}

	private JsonNode captureAccessLogEvent(String correlationId, ThrowingRunnable request) throws Exception {
		return captureAccessLogEventMatching(
				candidate -> correlationId.equals(candidate.path("correlationId").asString(null)),
				request);
	}

	private JsonNode captureAccessLogEventMatching(Predicate<JsonNode> matcher, ThrowingRunnable request) throws Exception {
		Logger rootLogger = (Logger) org.slf4j.LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		ListAppender<ILoggingEvent> capture = new ListAppender<>();
		capture.start();
		rootLogger.addAppender(capture);

		try {
			request.run();

			Encoder<ILoggingEvent> consoleEncoder = consoleEncoder(rootLogger);
			for (ILoggingEvent candidateEvent : capture.list) {
				byte[] encoded = consoleEncoder.encode(candidateEvent);
				JsonNode json = objectMapper.readTree(new String(encoded, StandardCharsets.UTF_8));
				if (matcher.test(json)) {
					return json;
				}
			}
			throw new AssertionError("no captured log event matched the expected predicate");
		} finally {
			rootLogger.detachAppender(capture);
			capture.stop();
		}
	}

	@SuppressWarnings("unchecked")
	private static Encoder<ILoggingEvent> consoleEncoder(Logger rootLogger) {
		Iterator<Appender<ILoggingEvent>> appenders = rootLogger.iteratorForAppenders();
		while (appenders.hasNext()) {
			Appender<ILoggingEvent> appender = appenders.next();
			if (appender instanceof OutputStreamAppender<ILoggingEvent> outputStreamAppender
					&& outputStreamAppender.getEncoder() != null) {
				return outputStreamAppender.getEncoder();
			}
		}
		throw new IllegalStateException("no console appender with a structured encoder found on the root logger");
	}

	@FunctionalInterface
	private interface ThrowingRunnable {
		void run() throws Exception;
	}

}
