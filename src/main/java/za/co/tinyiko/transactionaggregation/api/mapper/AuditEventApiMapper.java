package za.co.tinyiko.transactionaggregation.api.mapper;

import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import za.co.tinyiko.transactionaggregation.api.dto.response.AuditEventResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.AuditEventSearchResponse;
import za.co.tinyiko.transactionaggregation.audit.application.AuditEventSearchResult;
import za.co.tinyiko.transactionaggregation.audit.application.AuditEventView;

/**
 * Pure structural mapping between {@code api.dto} shapes and {@code audit.application} contracts
 * (feature/audit-query) - no business logic, matching every other mapper in this codebase, with
 * one deliberate exception: {@link #toResponse} takes an {@link ObjectMapper} parameter to parse
 * {@link AuditEventView#eventData()}'s raw JSON string into a {@link JsonNode}. This stays a
 * static utility method, not a Spring bean, exactly like every other {@code api.mapper} class -
 * the caller ({@code api.controller.AuditEventController}, already a Spring-managed component)
 * supplies the Boot-autoconfigured {@code ObjectMapper} it already holds via constructor
 * injection, rather than this mapper acquiring its own.
 *
 * <p>{@code objectMapper.readTree(String)} is deliberately called with no try/catch: {@code
 * event_data} is stored as PostgreSQL JSONB, so a parse failure here means the persisted data
 * itself is not valid JSON - a data-integrity/system condition, not a client input error. It must
 * propagate as a genuine unexpected failure (caught only by {@code GlobalExceptionHandler}'s
 * generic {@code Exception} handler, {@code 500 INTERNAL_SERVER_ERROR}), never silently
 * downgraded to a String fallback.
 */
public final class AuditEventApiMapper {

	private AuditEventApiMapper() {
	}

	public static AuditEventResponse toResponse(AuditEventView view, ObjectMapper objectMapper) {
		JsonNode eventData = objectMapper.readTree(view.eventData());
		return new AuditEventResponse(
				view.id(),
				view.aggregateType(),
				view.aggregateId(),
				view.eventType(),
				view.actor(),
				view.correlationId(),
				eventData,
				view.occurredAt());
	}

	public static AuditEventSearchResponse toSearchResponse(AuditEventSearchResult result, ObjectMapper objectMapper) {
		List<AuditEventResponse> content = result.content().stream()
				.map(view -> toResponse(view, objectMapper))
				.toList();
		AuditEventSearchResponse.PageInfo pageInfo = new AuditEventSearchResponse.PageInfo(
				result.page(), result.size(), result.totalElements(), result.totalPages());
		return new AuditEventSearchResponse(content, pageInfo);
	}

}
