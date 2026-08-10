package za.co.tinyiko.transactionaggregation.merchant.application;

import java.util.Objects;
import java.util.UUID;

/**
 * The response shape of {@link GetMerchantsUseCase} - a pure read of an already-known merchant
 * by id. Deliberately a distinct type from {@link MerchantResolutionResult}, even though the two
 * are structurally identical ({@code merchantId} + {@code displayName}): {@code MerchantResolutionResult}
 * is the outcome of a find-or-create operation that may write a row as a side effect;
 * {@code MerchantView} never does. Keeping the names distinct keeps that difference visible to a
 * future reader (for example in logs or stack traces), the same reasoning applied to keeping
 * {@code TransactionCreatedResult}/{@code TransactionDetails} separate in
 * {@code feature/transaction-query}.
 */
public record MerchantView(UUID merchantId, String displayName) {

	public MerchantView {
		Objects.requireNonNull(merchantId, "merchantId must not be null");
		Objects.requireNonNull(displayName, "displayName must not be null");
	}

}
