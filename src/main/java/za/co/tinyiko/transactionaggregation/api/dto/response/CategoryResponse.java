package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * The category-admin read response (feature/category-admin, TDS 56's named {@code CategoryResponse},
 * SAD 27.5's full attribute list). Read-only: no version, since no category write path exists in
 * this branch to make one meaningful.
 */
public record CategoryResponse(
		UUID id,
		String code,
		String name,
		String description,
		boolean fallback,
		boolean active,
		Instant createdAt,
		Instant updatedAt
) {
}
