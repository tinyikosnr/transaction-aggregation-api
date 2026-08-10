package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The audit-event search response envelope (feature/audit-query), matching the same
 * {@code content}/nested-{@code page} shape already established by
 * {@code TransactionSearchResponse}.
 */
public record AuditEventSearchResponse(
		@Schema(description = "Matching audit events for the requested page.") List<AuditEventResponse> content,
		@Schema(description = "Pagination metadata for this result.") PageInfo page
) {

	public record PageInfo(
			@Schema(description = "Zero-based page number returned.") int number,
			@Schema(description = "Requested page size.") int size,
			@Schema(description = "Total number of matching elements across all pages.") long totalElements,
			@Schema(description = "Total number of pages.") int totalPages
	) {
	}

}
