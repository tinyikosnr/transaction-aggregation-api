package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.util.List;

/**
 * The audit-event search response envelope (feature/audit-query), matching the same
 * {@code content}/nested-{@code page} shape already established by
 * {@code TransactionSearchResponse}.
 */
public record AuditEventSearchResponse(List<AuditEventResponse> content, PageInfo page) {

	public record PageInfo(int number, int size, long totalElements, int totalPages) {
	}

}
