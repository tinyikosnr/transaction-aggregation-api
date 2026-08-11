package za.co.tinyiko.transactionaggregation.api.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import za.co.tinyiko.transactionaggregation.api.dto.response.AuditEventSearchResponse;
import za.co.tinyiko.transactionaggregation.api.mapper.AuditEventApiMapper;
import za.co.tinyiko.transactionaggregation.audit.application.AuditEventSearchCriteria;
import za.co.tinyiko.transactionaggregation.audit.application.AuditEventSearchResult;
import za.co.tinyiko.transactionaggregation.audit.application.SearchAuditEventsUseCase;

/**
 * Audit-event search (feature/audit-query, SAD 31.2's "operational and compliance queries" -
 * the only documented basis for this capability). Thin: all validation,
 * blank-filter normalisation, and the "{@code aggregateId} requires {@code aggregateType}" rule
 * live in {@code audit.application.SearchAuditEventsService}, not here. The only endpoint in this
 * module - no get-by-id, no write/update/delete of any kind.
 */
@RestController
@Tag(name = "Audit", description = "Search recorded, append-only audit events (requires AUDIT_READ). No get-by-id endpoint and no write path - every audit event is recorded internally as a side effect of another operation.")
class AuditEventController {

	private final SearchAuditEventsUseCase searchAuditEventsUseCase;
	private final ObjectMapper objectMapper;

	AuditEventController(SearchAuditEventsUseCase searchAuditEventsUseCase, ObjectMapper objectMapper) {
		this.searchAuditEventsUseCase = searchAuditEventsUseCase;
		this.objectMapper = objectMapper;
	}

	@GetMapping("/api/v1/audit-events")
	@PreAuthorize("hasAuthority('AUDIT_READ')")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Search audit events", description = """
			Filtered, paginated, sorted search over append-only audit events. Every filter is optional and \
			combined with AND, except that aggregateId requires aggregateType to also be supplied. \
			occurredFrom/occurredTo filter occurredAt, both bounds inclusive when supplied. Requires the \
			AUDIT_READ authority.""")
	@ApiResponse(responseCode = "200", description = "Search executed (possibly with an empty page); see PageInfo for pagination metadata.")
	@ApiResponse(responseCode = "400", description = "REQUEST_VALIDATION_FAILED - invalid filter/sort value, or aggregateId supplied without aggregateType.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "401", description = "AUTHENTICATION_REQUIRED - missing or invalid bearer token.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "403", description = "ACCESS_DENIED - authenticated but missing AUDIT_READ.", content = @Content(mediaType = "application/problem+json"))
	AuditEventSearchResponse search(
			@Parameter(description = "Filter by aggregate type. Open vocabulary. Required when aggregateId is supplied.", example = "TRANSACTION")
			@RequestParam(required = false) String aggregateType,
			@Parameter(description = "Filter by aggregate id. Requires aggregateType to also be supplied.")
			@RequestParam(required = false) UUID aggregateId,
			@Parameter(description = "Filter by event type. Open vocabulary.", example = "TRANSACTION_CREATED")
			@RequestParam(required = false) String eventType,
			@Parameter(description = "Filter by actor.", example = "api-consumer-1") @RequestParam(required = false) String actor,
			@Parameter(description = "Filter by correlation id.") @RequestParam(required = false) String correlationId,
			@Parameter(description = "Inclusive lower bound on occurredAt (UTC).") @RequestParam(required = false) Instant occurredFrom,
			@Parameter(description = "Inclusive upper bound on occurredAt (UTC).") @RequestParam(required = false) Instant occurredTo,
			@Parameter(description = "Zero-based page number.") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Page size.") @RequestParam(defaultValue = "20") int size,
			@Parameter(description = "Sort as 'occurredAt,asc' or 'occurredAt,desc'.", example = "occurredAt,desc")
			@RequestParam(required = false) String sort
	) {
		AuditEventSearchCriteria criteria = new AuditEventSearchCriteria(
				aggregateType, aggregateId, eventType, actor, correlationId,
				occurredFrom, occurredTo, page, size, sort);
		AuditEventSearchResult result = searchAuditEventsUseCase.search(criteria);
		return AuditEventApiMapper.toSearchResponse(result, objectMapper);
	}

}
