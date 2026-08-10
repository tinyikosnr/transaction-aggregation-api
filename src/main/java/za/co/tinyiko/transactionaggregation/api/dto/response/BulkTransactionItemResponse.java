package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One row of the bulk-create response (SAD 35.2). {@code status} is one of {@code CREATED},
 * {@code CONFLICT}, {@code FAILED} - the only values the documentation's own example actually
 * shows ({@code CREATED}/{@code CONFLICT}); SAD 35.2/39.8 define no further status vocabulary, so
 * every other failure is reported as the generic {@code FAILED} bucket, with {@code errorCode}
 * (the same SAD 39.4 catalogue value used everywhere else in this API) carrying the specific
 * reason. {@code transactionId} is present only when {@code status} is {@code CREATED};
 * {@code errorCode}/{@code detail} only otherwise.
 */
public record BulkTransactionItemResponse(
		@Schema(description = "Zero-based index of this item within the submitted batch.") int index,

		@Schema(description = "Outcome of this item. CREATED means the transaction was persisted; CONFLICT means it was a duplicate (BR-08); FAILED is the generic bucket for every other failure, with errorCode carrying the specific reason.",
				allowableValues = {"CREATED", "CONFLICT", "FAILED"})
		String status,

		@Schema(description = "Identifier of the created transaction. Present only when status is CREATED.", nullable = true)
		UUID transactionId,

		@Schema(description = "SAD 39.4 error code identifying the specific failure reason. Present only when status is CONFLICT or FAILED.",
				example = "TRANSACTION_DUPLICATE", nullable = true)
		String errorCode,

		@Schema(description = "Human-readable detail for the failure. Present only when status is CONFLICT or FAILED.", nullable = true)
		String detail
) {
}
