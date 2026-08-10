package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.util.List;

/**
 * The transaction search response envelope, matching SAD 34.6's documented pagination shape
 * exactly ({@code content} + nested {@code page.number}/{@code size}/{@code totalElements}/
 * {@code totalPages}).
 */
public record TransactionSearchResponse(List<TransactionSearchItemResponse> content, PageInfo page) {

	public record PageInfo(int number, int size, long totalElements, int totalPages) {
	}

}
