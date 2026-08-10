package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The category-admin read response (feature/category-admin, TDS 56's named {@code CategoryResponse},
 * SAD 27.5's full attribute list). Read-only: no version, since no category write path exists in
 * this branch to make one meaningful.
 */
@Schema(description = "A transaction category, as seeded (V5). Read-only in this API - no create/update/delete path exists.")
public record CategoryResponse(
		@Schema(description = "Category identifier.") UUID id,
		@Schema(description = "Category code, referenced by rules and by the transaction search categoryCode filter.", example = "GROCERIES") String code,
		@Schema(description = "Category display name.", example = "Groceries") String name,
		@Schema(description = "Optional longer description.", nullable = true) String description,
		@Schema(description = "Whether this is the sole fallback category (SAD 27.5: at most one may be true) assigned when no rule matches.") boolean fallback,
		@Schema(description = "Whether this category is currently active.") boolean active,
		@Schema(description = "Timestamp the category was created, in UTC.") Instant createdAt,
		@Schema(description = "Timestamp the category was last updated, in UTC.") Instant updatedAt
) {
}
