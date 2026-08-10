package za.co.tinyiko.transactionaggregation.api.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.databind.ObjectMapper;

import za.co.tinyiko.transactionaggregation.api.dto.response.AuditEventSearchResponse;
import za.co.tinyiko.transactionaggregation.api.mapper.AuditEventApiMapper;
import za.co.tinyiko.transactionaggregation.audit.application.AuditEventSearchCriteria;
import za.co.tinyiko.transactionaggregation.audit.application.AuditEventSearchResult;
import za.co.tinyiko.transactionaggregation.audit.application.SearchAuditEventsUseCase;

/**
 * Audit-event search (feature/audit-query, SAD 31.2's "operational and compliance queries" -
 * the only documented basis for this capability; see CLAUDE.md for the full list of explicit
 * project decisions this endpoint's exact contract represents). Thin: all validation,
 * blank-filter normalisation, and the "{@code aggregateId} requires {@code aggregateType}" rule
 * live in {@code audit.application.SearchAuditEventsService}, not here. The only endpoint in this
 * module - no get-by-id, no write/update/delete of any kind.
 */
@RestController
class AuditEventController {

	private final SearchAuditEventsUseCase searchAuditEventsUseCase;
	private final ObjectMapper objectMapper;

	AuditEventController(SearchAuditEventsUseCase searchAuditEventsUseCase, ObjectMapper objectMapper) {
		this.searchAuditEventsUseCase = searchAuditEventsUseCase;
		this.objectMapper = objectMapper;
	}

	@GetMapping("/api/v1/audit-events")
	@PreAuthorize("hasAuthority('AUDIT_READ')")
	AuditEventSearchResponse search(
			@RequestParam(required = false) String aggregateType,
			@RequestParam(required = false) UUID aggregateId,
			@RequestParam(required = false) String eventType,
			@RequestParam(required = false) String actor,
			@RequestParam(required = false) String correlationId,
			@RequestParam(required = false) Instant occurredFrom,
			@RequestParam(required = false) Instant occurredTo,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String sort
	) {
		AuditEventSearchCriteria criteria = new AuditEventSearchCriteria(
				aggregateType, aggregateId, eventType, actor, correlationId,
				occurredFrom, occurredTo, page, size, sort);
		AuditEventSearchResult result = searchAuditEventsUseCase.search(criteria);
		return AuditEventApiMapper.toSearchResponse(result, objectMapper);
	}

}
