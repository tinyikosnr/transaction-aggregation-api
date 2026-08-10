package za.co.tinyiko.transactionaggregation.transaction.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.tinyiko.transactionaggregation.categorisation.application.CategoryView;
import za.co.tinyiko.transactionaggregation.categorisation.application.GetCategoryUseCase;
import za.co.tinyiko.transactionaggregation.merchant.application.GetMerchantsUseCase;
import za.co.tinyiko.transactionaggregation.merchant.application.MerchantView;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionDetailRow;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSearchRepositoryPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTransactionServiceTests {

	private static final UUID TRANSACTION_ID = UUID.randomUUID();
	private static final UUID CUSTOMER_ID = UUID.randomUUID();
	private static final UUID CATEGORY_ID = UUID.randomUUID();
	private static final UUID MERCHANT_ID = UUID.randomUUID();

	@Mock
	private TransactionSearchRepositoryPort transactionSearchRepositoryPort;
	@Mock
	private GetCategoryUseCase getCategoryUseCase;
	@Mock
	private GetMerchantsUseCase getMerchantsUseCase;

	private GetTransactionService service() {
		return new GetTransactionService(transactionSearchRepositoryPort, getCategoryUseCase, getMerchantsUseCase);
	}

	private static TransactionDetailRow aRow(UUID merchantId) {
		return new TransactionDetailRow(TRANSACTION_ID, CUSTOMER_ID, "MOCK_BANK_A", "EXT-001",
				merchantId, CATEGORY_ID, new BigDecimal("125.50"), "ZAR", "DEBIT", "groceries",
				Instant.parse("2026-08-06T08:00:00Z"), Instant.parse("2026-08-06T08:00:01Z"),
				"PROCESSED", Instant.parse("2026-08-06T08:00:01Z"));
	}

	@Test
	void returnsFullyEnrichedDetailsWhenFound() {
		when(transactionSearchRepositoryPort.findDetailById(TRANSACTION_ID)).thenReturn(Optional.of(aRow(MERCHANT_ID)));
		when(getCategoryUseCase.get(CATEGORY_ID)).thenReturn(new CategoryView(CATEGORY_ID, "GROCERIES", "Groceries"));
		when(getMerchantsUseCase.get(MERCHANT_ID)).thenReturn(Optional.of(new MerchantView(MERCHANT_ID, "Checkers")));

		TransactionDetails details = service().get(TRANSACTION_ID);

		assertThat(details.id()).isEqualTo(TRANSACTION_ID);
		assertThat(details.sourceCode()).isEqualTo("MOCK_BANK_A");
		assertThat(details.merchantId()).isEqualTo(MERCHANT_ID);
		assertThat(details.merchantDisplayName()).isEqualTo("Checkers");
		assertThat(details.categoryCode()).isEqualTo("GROCERIES");
		assertThat(details.categoryName()).isEqualTo("Groceries");
	}

	@Test
	void leavesMerchantFieldsNullWhenTheTransactionHasNoMerchant() {
		when(transactionSearchRepositoryPort.findDetailById(TRANSACTION_ID)).thenReturn(Optional.of(aRow(null)));
		when(getCategoryUseCase.get(CATEGORY_ID)).thenReturn(new CategoryView(CATEGORY_ID, "GROCERIES", "Groceries"));

		TransactionDetails details = service().get(TRANSACTION_ID);

		assertThat(details.merchantId()).isNull();
		assertThat(details.merchantDisplayName()).isNull();
	}

	@Test
	void throwsTransactionNotFoundWhenNoRowMatches() {
		when(transactionSearchRepositoryPort.findDetailById(TRANSACTION_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().get(TRANSACTION_ID)).isInstanceOf(TransactionNotFoundException.class);
	}

}
