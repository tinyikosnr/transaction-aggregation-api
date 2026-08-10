package za.co.tinyiko.transactionaggregation.audit.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.tinyiko.transactionaggregation.audit.port.AuditEventRow;
import za.co.tinyiko.transactionaggregation.audit.port.AuditEventSearchPage;
import za.co.tinyiko.transactionaggregation.audit.port.AuditEventSearchQuery;
import za.co.tinyiko.transactionaggregation.audit.port.AuditQueryRepositoryPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchAuditEventsServiceTests {

	private static final UUID AGGREGATE_ID = UUID.randomUUID();

	@Mock
	private AuditQueryRepositoryPort auditQueryRepositoryPort;

	private SearchAuditEventsService service() {
		return new SearchAuditEventsService(auditQueryRepositoryPort);
	}

	private static AuditEventSearchCriteria defaultCriteria() {
		return new AuditEventSearchCriteria(null, null, null, null, null, null, null, 0, 20, null);
	}

	private static AuditEventRow aRow() {
		return new AuditEventRow(UUID.randomUUID(), "TRANSACTION", AGGREGATE_ID, "TRANSACTION_CREATED",
				"api-consumer-1", "corr-1", "{\"foo\":\"bar\"}", Instant.parse("2026-08-15T08:00:00Z"));
	}

	@Test
	void defaultSearchWithNoFiltersDelegatesWithAllNullsAndDescendingOrder() {
		when(auditQueryRepositoryPort.search(any())).thenReturn(new AuditEventSearchPage(List.of(aRow()), 1));

		AuditEventSearchResult result = service().search(defaultCriteria());

		ArgumentCaptor<AuditEventSearchQuery> captor = ArgumentCaptor.forClass(AuditEventSearchQuery.class);
		verify(auditQueryRepositoryPort).search(captor.capture());
		AuditEventSearchQuery query = captor.getValue();
		assertThat(query.aggregateType()).isNull();
		assertThat(query.aggregateId()).isNull();
		assertThat(query.eventType()).isNull();
		assertThat(query.actor()).isNull();
		assertThat(query.correlationId()).isNull();
		assertThat(query.sortAscending()).isFalse();
		assertThat(result.content()).hasSize(1);
		assertThat(result.totalElements()).isEqualTo(1);
		assertThat(result.totalPages()).isEqualTo(1);
	}

	@Test
	void aggregateTypeFilterIsPassedThrough() {
		when(auditQueryRepositoryPort.search(any())).thenReturn(new AuditEventSearchPage(List.of(), 0));

		service().search(new AuditEventSearchCriteria("TRANSACTION", null, null, null, null, null, null, 0, 20, null));

		ArgumentCaptor<AuditEventSearchQuery> captor = ArgumentCaptor.forClass(AuditEventSearchQuery.class);
		verify(auditQueryRepositoryPort).search(captor.capture());
		assertThat(captor.getValue().aggregateType()).isEqualTo("TRANSACTION");
	}

	@Test
	void aggregateTypeAndAggregateIdTogetherArePassedThrough() {
		when(auditQueryRepositoryPort.search(any())).thenReturn(new AuditEventSearchPage(List.of(), 0));

		service().search(new AuditEventSearchCriteria("TRANSACTION", AGGREGATE_ID, null, null, null, null, null, 0, 20, null));

		ArgumentCaptor<AuditEventSearchQuery> captor = ArgumentCaptor.forClass(AuditEventSearchQuery.class);
		verify(auditQueryRepositoryPort).search(captor.capture());
		assertThat(captor.getValue().aggregateType()).isEqualTo("TRANSACTION");
		assertThat(captor.getValue().aggregateId()).isEqualTo(AGGREGATE_ID);
	}

	@Test
	void aggregateIdWithoutAggregateTypeThrowsAndNeverCallsThePort() {
		AuditEventSearchCriteria criteria = new AuditEventSearchCriteria(null, AGGREGATE_ID, null, null, null, null, null, 0, 20, null);

		assertThatThrownBy(() -> service().search(criteria)).isInstanceOf(AuditSearchValidationException.class);
		verify(auditQueryRepositoryPort, never()).search(any());
	}

	@Test
	void blankAggregateTypeWithAggregateIdIsTreatedAsAbsentAndThrows() {
		AuditEventSearchCriteria criteria = new AuditEventSearchCriteria("   ", AGGREGATE_ID, null, null, null, null, null, 0, 20, null);

		assertThatThrownBy(() -> service().search(criteria)).isInstanceOf(AuditSearchValidationException.class);
		verify(auditQueryRepositoryPort, never()).search(any());
	}

	@Test
	void eventTypeFilterIsPassedThrough() {
		when(auditQueryRepositoryPort.search(any())).thenReturn(new AuditEventSearchPage(List.of(), 0));

		service().search(new AuditEventSearchCriteria(null, null, "TRANSACTION_DUPLICATE_REJECTED", null, null, null, null, 0, 20, null));

		ArgumentCaptor<AuditEventSearchQuery> captor = ArgumentCaptor.forClass(AuditEventSearchQuery.class);
		verify(auditQueryRepositoryPort).search(captor.capture());
		assertThat(captor.getValue().eventType()).isEqualTo("TRANSACTION_DUPLICATE_REJECTED");
	}

	@Test
	void actorFilterIsPassedThrough() {
		when(auditQueryRepositoryPort.search(any())).thenReturn(new AuditEventSearchPage(List.of(), 0));

		service().search(new AuditEventSearchCriteria(null, null, null, "jwt-subject-001", null, null, null, 0, 20, null));

		ArgumentCaptor<AuditEventSearchQuery> captor = ArgumentCaptor.forClass(AuditEventSearchQuery.class);
		verify(auditQueryRepositoryPort).search(captor.capture());
		assertThat(captor.getValue().actor()).isEqualTo("jwt-subject-001");
	}

	@Test
	void correlationIdFilterIsPassedThrough() {
		when(auditQueryRepositoryPort.search(any())).thenReturn(new AuditEventSearchPage(List.of(), 0));

		service().search(new AuditEventSearchCriteria(null, null, null, null, "corr-1", null, null, 0, 20, null));

		ArgumentCaptor<AuditEventSearchQuery> captor = ArgumentCaptor.forClass(AuditEventSearchQuery.class);
		verify(auditQueryRepositoryPort).search(captor.capture());
		assertThat(captor.getValue().correlationId()).isEqualTo("corr-1");
	}

	@Test
	void blankStringFiltersNormaliseToAbsent() {
		when(auditQueryRepositoryPort.search(any())).thenReturn(new AuditEventSearchPage(List.of(), 0));

		service().search(new AuditEventSearchCriteria("", null, "   ", "", "  ", null, null, 0, 20, null));

		ArgumentCaptor<AuditEventSearchQuery> captor = ArgumentCaptor.forClass(AuditEventSearchQuery.class);
		verify(auditQueryRepositoryPort).search(captor.capture());
		AuditEventSearchQuery query = captor.getValue();
		assertThat(query.aggregateType()).isNull();
		assertThat(query.eventType()).isNull();
		assertThat(query.actor()).isNull();
		assertThat(query.correlationId()).isNull();
	}

	@Test
	void occurredFromAndToArePassedThrough() {
		when(auditQueryRepositoryPort.search(any())).thenReturn(new AuditEventSearchPage(List.of(), 0));
		Instant from = Instant.parse("2026-01-01T00:00:00Z");
		Instant to = Instant.parse("2026-01-31T23:59:59Z");

		service().search(new AuditEventSearchCriteria(null, null, null, null, null, from, to, 0, 20, null));

		ArgumentCaptor<AuditEventSearchQuery> captor = ArgumentCaptor.forClass(AuditEventSearchQuery.class);
		verify(auditQueryRepositoryPort).search(captor.capture());
		assertThat(captor.getValue().occurredFrom()).isEqualTo(from);
		assertThat(captor.getValue().occurredTo()).isEqualTo(to);
	}

	@Test
	void occurredFromAfterOccurredToThrows() {
		Instant from = Instant.parse("2026-01-31T00:00:00Z");
		Instant to = Instant.parse("2026-01-01T00:00:00Z");
		AuditEventSearchCriteria criteria = new AuditEventSearchCriteria(null, null, null, null, null, from, to, 0, 20, null);

		assertThatThrownBy(() -> service().search(criteria)).isInstanceOf(AuditSearchValidationException.class);
		verify(auditQueryRepositoryPort, never()).search(any());
	}

	@Test
	void combinedFiltersAreAllPassedThroughTogether() {
		when(auditQueryRepositoryPort.search(any())).thenReturn(new AuditEventSearchPage(List.of(), 0));
		Instant from = Instant.parse("2026-01-01T00:00:00Z");
		Instant to = Instant.parse("2026-01-31T23:59:59Z");

		service().search(new AuditEventSearchCriteria("TRANSACTION", AGGREGATE_ID, "TRANSACTION_CREATED",
				"api-consumer-1", "corr-1", from, to, 1, 10, "occurredAt,asc"));

		ArgumentCaptor<AuditEventSearchQuery> captor = ArgumentCaptor.forClass(AuditEventSearchQuery.class);
		verify(auditQueryRepositoryPort).search(captor.capture());
		AuditEventSearchQuery query = captor.getValue();
		assertThat(query.aggregateType()).isEqualTo("TRANSACTION");
		assertThat(query.aggregateId()).isEqualTo(AGGREGATE_ID);
		assertThat(query.eventType()).isEqualTo("TRANSACTION_CREATED");
		assertThat(query.actor()).isEqualTo("api-consumer-1");
		assertThat(query.correlationId()).isEqualTo("corr-1");
		assertThat(query.occurredFrom()).isEqualTo(from);
		assertThat(query.occurredTo()).isEqualTo(to);
		assertThat(query.page()).isEqualTo(1);
		assertThat(query.size()).isEqualTo(10);
		assertThat(query.sortAscending()).isTrue();
	}

	@Test
	void negativePageThrows() {
		AuditEventSearchCriteria criteria = new AuditEventSearchCriteria(null, null, null, null, null, null, null, -1, 20, null);

		assertThatThrownBy(() -> service().search(criteria)).isInstanceOf(AuditSearchValidationException.class);
	}

	@Test
	void sizeAboveMaximumThrows() {
		AuditEventSearchCriteria criteria = new AuditEventSearchCriteria(null, null, null, null, null, null, null, 0, 101, null);

		assertThatThrownBy(() -> service().search(criteria)).isInstanceOf(AuditSearchValidationException.class);
	}

	@Test
	void sizeAtMaximumIsAccepted() {
		when(auditQueryRepositoryPort.search(any())).thenReturn(new AuditEventSearchPage(List.of(), 0));
		AuditEventSearchCriteria criteria = new AuditEventSearchCriteria(null, null, null, null, null, null, null, 0, 100, null);

		service().search(criteria);

		verify(auditQueryRepositoryPort).search(any());
	}

	@Test
	void zeroSizeThrows() {
		AuditEventSearchCriteria criteria = new AuditEventSearchCriteria(null, null, null, null, null, null, null, 0, 0, null);

		assertThatThrownBy(() -> service().search(criteria)).isInstanceOf(AuditSearchValidationException.class);
	}

	@Test
	void unsupportedSortFieldThrows() {
		AuditEventSearchCriteria criteria = new AuditEventSearchCriteria(null, null, null, null, null, null, null, 0, 20, "actor,asc");

		assertThatThrownBy(() -> service().search(criteria)).isInstanceOf(AuditSearchValidationException.class);
	}

	@Test
	void unsupportedSortDirectionThrows() {
		AuditEventSearchCriteria criteria = new AuditEventSearchCriteria(null, null, null, null, null, null, null, 0, 20, "occurredAt,sideways");

		assertThatThrownBy(() -> service().search(criteria)).isInstanceOf(AuditSearchValidationException.class);
	}

	@Test
	void explicitAscendingSortIsHonoured() {
		when(auditQueryRepositoryPort.search(any())).thenReturn(new AuditEventSearchPage(List.of(), 0));
		AuditEventSearchCriteria criteria = new AuditEventSearchCriteria(null, null, null, null, null, null, null, 0, 20, "occurredAt,asc");

		service().search(criteria);

		ArgumentCaptor<AuditEventSearchQuery> captor = ArgumentCaptor.forClass(AuditEventSearchQuery.class);
		verify(auditQueryRepositoryPort).search(captor.capture());
		assertThat(captor.getValue().sortAscending()).isTrue();
	}

	@Test
	void emptyResultProducesZeroTotalsAndZeroPages() {
		when(auditQueryRepositoryPort.search(any())).thenReturn(new AuditEventSearchPage(List.of(), 0));

		AuditEventSearchResult result = service().search(defaultCriteria());

		assertThat(result.content()).isEmpty();
		assertThat(result.totalElements()).isZero();
		assertThat(result.totalPages()).isZero();
	}

	@Test
	void eventDataPassesThroughToTheViewUnparsed() {
		when(auditQueryRepositoryPort.search(any())).thenReturn(new AuditEventSearchPage(List.of(aRow()), 1));

		AuditEventSearchResult result = service().search(defaultCriteria());

		assertThat(result.content().get(0).eventData()).isEqualTo("{\"foo\":\"bar\"}");
	}

}
