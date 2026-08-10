package za.co.tinyiko.transactionaggregation.transaction.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.tinyiko.transactionaggregation.categorisation.application.CategoryView;
import za.co.tinyiko.transactionaggregation.categorisation.application.GetCategoryUseCase;
import za.co.tinyiko.transactionaggregation.merchant.application.GetMerchantsUseCase;
import za.co.tinyiko.transactionaggregation.merchant.application.MerchantView;
import za.co.tinyiko.transactionaggregation.transaction.domain.SourceStatus;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionSource;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionSourceId;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSearchPage;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSearchQuery;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSearchRepositoryPort;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSearchRow;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSourceRepositoryPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchTransactionsServiceTests {

	private static final UUID CUSTOMER_ID = UUID.randomUUID();
	private static final UUID CATEGORY_ID = UUID.randomUUID();
	private static final UUID MERCHANT_ID = UUID.randomUUID();
	private static final UUID SOURCE_ID = UUID.randomUUID();

	@Mock
	private TransactionSearchRepositoryPort transactionSearchRepositoryPort;
	@Mock
	private TransactionSourceRepositoryPort transactionSourceRepositoryPort;
	@Mock
	private GetCategoryUseCase getCategoryUseCase;
	@Mock
	private GetMerchantsUseCase getMerchantsUseCase;

	private SearchTransactionsService service() {
		return new SearchTransactionsService(transactionSearchRepositoryPort, transactionSourceRepositoryPort, getCategoryUseCase, getMerchantsUseCase);
	}

	private static TransactionSearchCriteria defaultCriteria() {
		return new TransactionSearchCriteria(null, null, null, null, null, null, null, null, 0, 20, null);
	}

	private static TransactionSearchRow aRow() {
		return new TransactionSearchRow(UUID.randomUUID(), CUSTOMER_ID, MERCHANT_ID, CATEGORY_ID,
				new BigDecimal("125.50"), "ZAR", "DEBIT", Instant.parse("2026-08-06T08:00:00Z"));
	}

	@Test
	void searchesWithNoFiltersAndDefaultSortDescending() {
		when(transactionSearchRepositoryPort.search(any())).thenReturn(new TransactionSearchPage(List.of(), 0));

		service().search(defaultCriteria());

		ArgumentCaptor<TransactionSearchQuery> captor = ArgumentCaptor.forClass(TransactionSearchQuery.class);
		verify(transactionSearchRepositoryPort).search(captor.capture());
		assertThat(captor.getValue().sortAscending()).isFalse();
		assertThat(captor.getValue().customerId()).isNull();
	}

	@Test
	void passesCustomerIdMerchantIdDirectionAndStatusThroughUnchanged() {
		when(transactionSearchRepositoryPort.search(any())).thenReturn(new TransactionSearchPage(List.of(), 0));
		TransactionSearchCriteria criteria = new TransactionSearchCriteria(
				CUSTOMER_ID, null, null, MERCHANT_ID, "DEBIT", "PROCESSED", null, null, 0, 20, null);

		service().search(criteria);

		ArgumentCaptor<TransactionSearchQuery> captor = ArgumentCaptor.forClass(TransactionSearchQuery.class);
		verify(transactionSearchRepositoryPort).search(captor.capture());
		assertThat(captor.getValue().customerId()).isEqualTo(CUSTOMER_ID);
		assertThat(captor.getValue().merchantId()).isEqualTo(MERCHANT_ID);
		assertThat(captor.getValue().direction()).isEqualTo("DEBIT");
		assertThat(captor.getValue().status()).isEqualTo("PROCESSED");
	}

	@Test
	void resolvesSourceCodeToTransactionSourceId() {
		TransactionSource source = TransactionSource.register(new TransactionSourceId(SOURCE_ID), "MOCK_BANK_A",
				"Mock Bank A", SourceStatus.ACTIVE, java.time.Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
		when(transactionSourceRepositoryPort.findByCode("MOCK_BANK_A")).thenReturn(Optional.of(source));
		when(transactionSearchRepositoryPort.search(any())).thenReturn(new TransactionSearchPage(List.of(), 0));
		TransactionSearchCriteria criteria = new TransactionSearchCriteria(
				null, "MOCK_BANK_A", null, null, null, null, null, null, 0, 20, null);

		service().search(criteria);

		ArgumentCaptor<TransactionSearchQuery> captor = ArgumentCaptor.forClass(TransactionSearchQuery.class);
		verify(transactionSearchRepositoryPort).search(captor.capture());
		assertThat(captor.getValue().transactionSourceId()).isEqualTo(SOURCE_ID);
	}

	@Test
	void anUnresolvableSourceCodeYieldsAGuaranteedNonMatchingFilterNotNoFilterAtAll() {
		when(transactionSourceRepositoryPort.findByCode("UNKNOWN")).thenReturn(Optional.empty());
		when(transactionSearchRepositoryPort.search(any())).thenReturn(new TransactionSearchPage(List.of(), 0));
		TransactionSearchCriteria criteria = new TransactionSearchCriteria(
				null, "UNKNOWN", null, null, null, null, null, null, 0, 20, null);

		service().search(criteria);

		ArgumentCaptor<TransactionSearchQuery> captor = ArgumentCaptor.forClass(TransactionSearchQuery.class);
		verify(transactionSearchRepositoryPort).search(captor.capture());
		assertThat(captor.getValue().transactionSourceId()).isNotNull();
	}

	@Test
	void resolvesCategoryCodeToCategoryId() {
		when(getCategoryUseCase.findIdByCode("GROCERIES")).thenReturn(Optional.of(CATEGORY_ID));
		when(transactionSearchRepositoryPort.search(any())).thenReturn(new TransactionSearchPage(List.of(), 0));
		TransactionSearchCriteria criteria = new TransactionSearchCriteria(
				null, null, "GROCERIES", null, null, null, null, null, 0, 20, null);

		service().search(criteria);

		ArgumentCaptor<TransactionSearchQuery> captor = ArgumentCaptor.forClass(TransactionSearchQuery.class);
		verify(transactionSearchRepositoryPort).search(captor.capture());
		assertThat(captor.getValue().categoryId()).isEqualTo(CATEGORY_ID);
	}

	@Test
	void anUnresolvableCategoryCodeYieldsAGuaranteedNonMatchingFilter() {
		when(getCategoryUseCase.findIdByCode("UNKNOWN")).thenReturn(Optional.empty());
		when(transactionSearchRepositoryPort.search(any())).thenReturn(new TransactionSearchPage(List.of(), 0));
		TransactionSearchCriteria criteria = new TransactionSearchCriteria(
				null, null, "UNKNOWN", null, null, null, null, null, 0, 20, null);

		service().search(criteria);

		ArgumentCaptor<TransactionSearchQuery> captor = ArgumentCaptor.forClass(TransactionSearchQuery.class);
		verify(transactionSearchRepositoryPort).search(captor.capture());
		assertThat(captor.getValue().categoryId()).isNotNull();
	}

	@Test
	void enrichesAPageOfResultsWithExactlyOneBatchCallEachRegardlessOfRowCount() {
		List<TransactionSearchRow> rows = List.of(aRow(), aRow(), aRow());
		when(transactionSearchRepositoryPort.search(any())).thenReturn(new TransactionSearchPage(rows, 3));
		when(getMerchantsUseCase.findByIds(Set.of(MERCHANT_ID))).thenReturn(List.of(new MerchantView(MERCHANT_ID, "Checkers")));
		when(getCategoryUseCase.getByIds(Set.of(CATEGORY_ID))).thenReturn(List.of(new CategoryView(CATEGORY_ID, "GROCERIES", "Groceries")));

		PagedResult<TransactionSearchResultItem> result = service().search(defaultCriteria());

		assertThat(result.content()).hasSize(3);
		assertThat(result.content()).allSatisfy(item -> {
			assertThat(item.merchantName()).isEqualTo("Checkers");
			assertThat(item.categoryCode()).isEqualTo("GROCERIES");
		});
		verify(getMerchantsUseCase, times(1)).findByIds(any());
		verify(getCategoryUseCase, times(1)).getByIds(any());
	}

	@Test
	void skipsEnrichmentCallsWhenTheResultPageIsEmpty() {
		when(transactionSearchRepositoryPort.search(any())).thenReturn(new TransactionSearchPage(List.of(), 0));

		service().search(defaultCriteria());

		verify(getMerchantsUseCase, never()).findByIds(any());
		verify(getCategoryUseCase, never()).getByIds(any());
	}

	@Test
	void computesTotalPagesFromTotalElementsAndSize() {
		when(transactionSearchRepositoryPort.search(any())).thenReturn(new TransactionSearchPage(List.of(), 45));
		TransactionSearchCriteria criteria = new TransactionSearchCriteria(null, null, null, null, null, null, null, null, 0, 20, null);

		PagedResult<TransactionSearchResultItem> result = service().search(criteria);

		assertThat(result.totalElements()).isEqualTo(45);
		assertThat(result.totalPages()).isEqualTo(3);
	}

	@Test
	void ascendingSortIsRecognised() {
		when(transactionSearchRepositoryPort.search(any())).thenReturn(new TransactionSearchPage(List.of(), 0));
		TransactionSearchCriteria criteria = new TransactionSearchCriteria(null, null, null, null, null, null, null, null, 0, 20, "transactionTimestamp,asc");

		service().search(criteria);

		ArgumentCaptor<TransactionSearchQuery> captor = ArgumentCaptor.forClass(TransactionSearchQuery.class);
		verify(transactionSearchRepositoryPort).search(captor.capture());
		assertThat(captor.getValue().sortAscending()).isTrue();
	}

	@Test
	void rejectsAnUnsupportedSortField() {
		TransactionSearchCriteria criteria = new TransactionSearchCriteria(null, null, null, null, null, null, null, null, 0, 20, "amount,desc");

		assertThatThrownBy(() -> service().search(criteria)).isInstanceOf(TransactionValidationException.class);
	}

	@Test
	void rejectsAnUnsupportedSortDirection() {
		TransactionSearchCriteria criteria = new TransactionSearchCriteria(null, null, null, null, null, null, null, null, 0, 20, "transactionTimestamp,sideways");

		assertThatThrownBy(() -> service().search(criteria)).isInstanceOf(TransactionValidationException.class);
	}

	@Test
	void rejectsANegativePage() {
		TransactionSearchCriteria criteria = new TransactionSearchCriteria(null, null, null, null, null, null, null, null, -1, 20, null);

		assertThatThrownBy(() -> service().search(criteria)).isInstanceOf(TransactionValidationException.class);
	}

	@Test
	void rejectsASizeAboveTheMaximum() {
		TransactionSearchCriteria criteria = new TransactionSearchCriteria(null, null, null, null, null, null, null, null, 0, 101, null);

		assertThatThrownBy(() -> service().search(criteria)).isInstanceOf(TransactionValidationException.class);
	}

	@Test
	void rejectsASizeBelowOne() {
		TransactionSearchCriteria criteria = new TransactionSearchCriteria(null, null, null, null, null, null, null, null, 0, 0, null);

		assertThatThrownBy(() -> service().search(criteria)).isInstanceOf(TransactionValidationException.class);
	}

	@Test
	void rejectsAnInvalidDirection() {
		TransactionSearchCriteria criteria = new TransactionSearchCriteria(null, null, null, null, "SIDEWAYS", null, null, null, 0, 20, null);

		assertThatThrownBy(() -> service().search(criteria)).isInstanceOf(TransactionValidationException.class);
	}

	@Test
	void rejectsAnInvalidStatus() {
		TransactionSearchCriteria criteria = new TransactionSearchCriteria(null, null, null, null, null, "UNKNOWN", null, null, 0, 20, null);

		assertThatThrownBy(() -> service().search(criteria)).isInstanceOf(TransactionValidationException.class);
	}

	@Test
	void rejectsOccurredFromAfterOccurredTo() {
		TransactionSearchCriteria criteria = new TransactionSearchCriteria(null, null, null, null, null, null,
				Instant.parse("2026-02-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"), 0, 20, null);

		assertThatThrownBy(() -> service().search(criteria)).isInstanceOf(TransactionValidationException.class);
	}

	@Test
	void rejectsADateRangeSpanningMoreThanTwentyFourMonths() {
		TransactionSearchCriteria criteria = new TransactionSearchCriteria(null, null, null, null, null, null,
				Instant.parse("2024-01-01T00:00:00Z"), Instant.parse("2026-02-01T00:00:00Z"), 0, 20, null);

		assertThatThrownBy(() -> service().search(criteria)).isInstanceOf(TransactionValidationException.class);
	}

	@Test
	void acceptsAOneSidedDateRangeWithoutASpanCheck() {
		when(transactionSearchRepositoryPort.search(any())).thenReturn(new TransactionSearchPage(List.of(), 0));
		TransactionSearchCriteria criteria = new TransactionSearchCriteria(null, null, null, null, null, null,
				Instant.parse("2020-01-01T00:00:00Z"), null, 0, 20, null);

		service().search(criteria);

		ArgumentCaptor<TransactionSearchQuery> captor = ArgumentCaptor.forClass(TransactionSearchQuery.class);
		verify(transactionSearchRepositoryPort).search(captor.capture());
		assertThat(captor.getValue().occurredFrom()).isEqualTo(Instant.parse("2020-01-01T00:00:00Z"));
		assertThat(captor.getValue().occurredTo()).isNull();
	}

}
