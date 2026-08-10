package za.co.tinyiko.transactionaggregation.api.dto.response;

import java.util.UUID;

/**
 * One row of the bulk-create response (SAD 35.2). {@code status} is one of {@code CREATED},
 * {@code CONFLICT}, {@code FAILED} - the only values the documentation's own example actually
 * shows ({@code CREATED}/{@code CONFLICT}); SAD 35.2/39.8 define no further status vocabulary, so
 * every other failure is reported as the generic {@code FAILED} bucket, with {@code errorCode}
 * (the same SAD 39.4 catalogue value used everywhere else in this API) carrying the specific
 * reason. {@code transactionId} is present only when {@code status} is {@code CREATED};
 * {@code errorCode}/{@code detail} only otherwise.
 */
public record BulkTransactionItemResponse(int index, String status, UUID transactionId, String errorCode, String detail) {
}
