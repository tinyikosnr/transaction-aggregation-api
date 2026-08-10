package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The transaction search response envelope, matching SAD 34.6's documented pagination shape
 * exactly ({@code content} + nested {@code page.number}/{@code size}/{@code totalElements}/
 * {@code totalPages}).
 */
public record TransactionSearchResponse(
		@Schema(description = "Matching transactions for the requested page.") List<TransactionSearchItemResponse> content,
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
