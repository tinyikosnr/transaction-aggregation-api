package za.co.tinyiko.transactionaggregation.transaction.application;

import org.springframework.stereotype.Service;

import java.util.UUID;

import za.co.tinyiko.transactionaggregation.categorisation.application.CategoryView;
import za.co.tinyiko.transactionaggregation.categorisation.application.GetCategoryUseCase;
import za.co.tinyiko.transactionaggregation.merchant.application.GetMerchantsUseCase;
import za.co.tinyiko.transactionaggregation.merchant.application.MerchantView;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionDetailRow;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSearchRepositoryPort;

/**
 * Enrichment flow: one row fetch (with {@code sourceCode} already resolved via an intra-module
 * join at the persistence layer), one single-item category lookup, one single-item merchant
 * lookup - three queries total, fixed regardless of anything, since this is a single-row
 * operation with no batching concern.
 */
@Service
class GetTransactionService implements GetTransactionUseCase {

	private final TransactionSearchRepositoryPort transactionSearchRepositoryPort;
	private final GetCategoryUseCase getCategoryUseCase;
	private final GetMerchantsUseCase getMerchantsUseCase;

	GetTransactionService(
			TransactionSearchRepositoryPort transactionSearchRepositoryPort,
			GetCategoryUseCase getCategoryUseCase,
			GetMerchantsUseCase getMerchantsUseCase
	) {
		this.transactionSearchRepositoryPort = transactionSearchRepositoryPort;
		this.getCategoryUseCase = getCategoryUseCase;
		this.getMerchantsUseCase = getMerchantsUseCase;
	}

	@Override
	public TransactionDetails get(UUID transactionId) {
		TransactionDetailRow row = transactionSearchRepositoryPort.findDetailById(transactionId)
				.orElseThrow(() -> new TransactionNotFoundException(transactionId));

		CategoryView category = getCategoryUseCase.get(row.categoryId());
		MerchantView merchant = row.merchantId() == null
				? null
				: getMerchantsUseCase.get(row.merchantId()).orElse(null);

		return new TransactionDetails(
				row.id(),
				row.customerId(),
				row.sourceCode(),
				row.externalTransactionId(),
				merchant == null ? null : merchant.merchantId(),
				merchant == null ? null : merchant.displayName(),
				category.code(),
				category.name(),
				row.amount(),
				row.currency(),
				row.direction(),
				row.description(),
				row.status(),
				row.occurredAt(),
				row.receivedAt(),
				row.createdAt());
	}

}
