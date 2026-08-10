package za.co.tinyiko.transactionaggregation.api.mapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

import za.co.tinyiko.transactionaggregation.api.dto.response.AuditEventResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.AuditEventSearchResponse;
import za.co.tinyiko.transactionaggregation.audit.application.AuditEventSearchResult;
import za.co.tinyiko.transactionaggregation.audit.application.AuditEventView;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves {@code eventData} is embedded as real, nested JSON in the serialized HTTP response, not
 * a re-escaped JSON string - the exact risk {@code AuditEventApiMapper}'s Javadoc calls out.
 */
class AuditEventApiMapperTests {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private static AuditEventView aView(String eventData) {
		return new AuditEventView(UUID.randomUUID(), "TRANSACTION", UUID.randomUUID(), "TRANSACTION_CREATED",
				"api-consumer-1", "corr-1", eventData, Instant.parse("2026-08-15T08:00:00Z"));
	}

	@Test
	void eventDataIsParsedIntoAJsonNodeNotLeftAsAString() {
		AuditEventResponse response = AuditEventApiMapper.toResponse(aView("{\"foo\":\"bar\"}"), OBJECT_MAPPER);

		assertThat(response.eventData().isObject()).isTrue();
		assertThat(response.eventData().get("foo").asString()).isEqualTo("bar");
	}

	@Test
	void serializedResponseEmbedsEventDataAsANestedObjectNotAnEscapedString() {
		AuditEventResponse response = AuditEventApiMapper.toResponse(aView("{\"foo\":\"bar\"}"), OBJECT_MAPPER);

		String json = OBJECT_MAPPER.writeValueAsString(response);

		assertThat(json).contains("\"eventData\":{\"foo\":\"bar\"}");
		assertThat(json).doesNotContain("\"eventData\":\"{");
	}

	@Test
	void toSearchResponseMapsEveryItemAndPreservesPageInfo() {
		AuditEventSearchResult result = new AuditEventSearchResult(
				List.of(aView("{\"a\":1}"), aView("{\"b\":2}")), 0, 20, 2, 1);

		AuditEventSearchResponse response = AuditEventApiMapper.toSearchResponse(result, OBJECT_MAPPER);

		assertThat(response.content()).hasSize(2);
		assertThat(response.content().get(0).eventData().get("a").asInt()).isEqualTo(1);
		assertThat(response.page().number()).isZero();
		assertThat(response.page().size()).isEqualTo(20);
		assertThat(response.page().totalElements()).isEqualTo(2);
		assertThat(response.page().totalPages()).isEqualTo(1);
	}

	@Test
	void malformedEventDataPropagatesAsAnUnexpectedFailureRatherThanFallingBackToAString() {
		try {
			AuditEventApiMapper.toResponse(aView("not-valid-json"), OBJECT_MAPPER);
			org.junit.jupiter.api.Assertions.fail("expected a Jackson parse failure to propagate");
		} catch (RuntimeException expected) {
			// propagation confirmed - not caught/swallowed inside the mapper, matching
			// GlobalExceptionHandler's generic Exception -> 500 INTERNAL_SERVER_ERROR handling
			assertThat(expected).isNotNull();
		}
	}

}
