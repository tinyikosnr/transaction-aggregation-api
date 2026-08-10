package za.co.tinyiko.transactionaggregation.audit.persistence;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import za.co.tinyiko.transactionaggregation.TestcontainersConfiguration;
import za.co.tinyiko.transactionaggregation.audit.domain.AuditEvent;
import za.co.tinyiko.transactionaggregation.audit.domain.AuditEventId;
import za.co.tinyiko.transactionaggregation.audit.mapper.AuditMapper;
import za.co.tinyiko.transactionaggregation.audit.port.AuditEventRow;
import za.co.tinyiko.transactionaggregation.audit.port.AuditEventSearchPage;
import za.co.tinyiko.transactionaggregation.audit.port.AuditEventSearchQuery;
import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the port -> adapter -> JPA -> Testcontainers-PostgreSQL chain for the read/search
 * side, including {@code SpringDataAuditSearchRepository}'s {@code Repository +
 * JpaSpecificationExecutor} combination actually working for {@code findAll(spec, pageable)}
 * against real Postgres, not just compiling.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaAuditQueryRepositoryAdapterTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-15T08:00:00Z"), ZoneOffset.UTC);
	private static final UUID TRANSACTION_AGGREGATE_ID = UUID.randomUUID();
	private static final UUID CUSTOMER_AGGREGATE_ID = UUID.randomUUID();

	@Autowired
	private SpringDataAuditRepository springDataAuditRepository;

	@Autowired
	private SpringDataAuditSearchRepository springDataAuditSearchRepository;

	private JpaAuditQueryRepositoryAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new JpaAuditQueryRepositoryAdapter(springDataAuditSearchRepository);

		insert("TRANSACTION", TRANSACTION_AGGREGATE_ID, "TRANSACTION_CREATED", "api-consumer-1", "corr-1",
				"{\"reason\":\"created\"}", Instant.parse("2026-01-01T00:00:00Z"));
		insert("TRANSACTION", TRANSACTION_AGGREGATE_ID, "TRANSACTION_DUPLICATE_REJECTED", "api-consumer-1", "corr-2",
				"{\"reason\":\"duplicate\"}", Instant.parse("2026-01-15T00:00:00Z"));
		insert("CUSTOMER", CUSTOMER_AGGREGATE_ID, "CUSTOMER_REGISTERED", "system", "corr-3",
				"{\"reason\":\"registered\"}", Instant.parse("2026-01-31T23:59:59Z"));
	}

	private void insert(String aggregateType, UUID aggregateId, String eventType, String actor,
			String correlationId, String eventData, Instant occurredAt) {
		Clock instantClock = Clock.fixed(occurredAt, ZoneOffset.UTC);
		AuditEvent event = AuditEvent.register(AuditEventId.generate(), aggregateType, aggregateId, eventType,
				actor, new CorrelationId(correlationId), eventData, instantClock);
		springDataAuditRepository.save(AuditMapper.toEntity(event));
	}

	private static AuditEventSearchQuery emptyQuery(int page, int size, boolean ascending) {
		return new AuditEventSearchQuery(null, null, null, null, null, null, null, page, size, ascending);
	}

	@Test
	void searchWithNoFiltersReturnsEveryRow() {
		AuditEventSearchPage page = adapter.search(emptyQuery(0, 20, false));

		assertThat(page.totalElements()).isEqualTo(3);
		assertThat(page.rows()).hasSize(3);
	}

	@Test
	void filtersByAggregateTypeAlone() {
		AuditEventSearchQuery query = new AuditEventSearchQuery("TRANSACTION", null, null, null, null, null, null, 0, 20, false);

		AuditEventSearchPage page = adapter.search(query);

		assertThat(page.rows()).hasSize(2);
		assertThat(page.rows()).allMatch(row -> row.aggregateType().equals("TRANSACTION"));
	}

	@Test
	void filtersByAggregateTypeAndAggregateIdTogether() {
		AuditEventSearchQuery query = new AuditEventSearchQuery("CUSTOMER", CUSTOMER_AGGREGATE_ID, null, null, null, null, null, 0, 20, false);

		AuditEventSearchPage page = adapter.search(query);

		assertThat(page.rows()).hasSize(1);
		assertThat(page.rows().get(0).eventType()).isEqualTo("CUSTOMER_REGISTERED");
	}

	@Test
	void filtersByEventType() {
		AuditEventSearchQuery query = new AuditEventSearchQuery(null, null, "TRANSACTION_DUPLICATE_REJECTED", null, null, null, null, 0, 20, false);

		AuditEventSearchPage page = adapter.search(query);

		assertThat(page.rows()).hasSize(1);
		assertThat(page.rows().get(0).correlationId()).isEqualTo("corr-2");
	}

	@Test
	void filtersByActor() {
		AuditEventSearchQuery query = new AuditEventSearchQuery(null, null, null, "system", null, null, null, 0, 20, false);

		AuditEventSearchPage page = adapter.search(query);

		assertThat(page.rows()).hasSize(1);
		assertThat(page.rows().get(0).eventType()).isEqualTo("CUSTOMER_REGISTERED");
	}

	@Test
	void filtersByCorrelationId() {
		AuditEventSearchQuery query = new AuditEventSearchQuery(null, null, null, null, "corr-1", null, null, 0, 20, false);

		AuditEventSearchPage page = adapter.search(query);

		assertThat(page.rows()).hasSize(1);
		assertThat(page.rows().get(0).eventType()).isEqualTo("TRANSACTION_CREATED");
	}

	@Test
	void filtersByOccurredAtRangeInclusiveOnBothBoundaries() {
		AuditEventSearchQuery query = new AuditEventSearchQuery(null, null, null, null, null,
				Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-15T00:00:00Z"), 0, 20, false);

		AuditEventSearchPage page = adapter.search(query);

		assertThat(page.rows()).hasSize(2);
	}

	@Test
	void combinesMultipleFiltersWithAnd() {
		AuditEventSearchQuery query = new AuditEventSearchQuery("TRANSACTION", TRANSACTION_AGGREGATE_ID,
				"TRANSACTION_CREATED", "api-consumer-1", null, null, null, 0, 20, false);

		AuditEventSearchPage page = adapter.search(query);

		assertThat(page.rows()).hasSize(1);
		assertThat(page.rows().get(0).correlationId()).isEqualTo("corr-1");
	}

	@Test
	void paginatesCorrectlyAcrossMultiplePages() {
		AuditEventSearchPage firstPage = adapter.search(emptyQuery(0, 2, false));
		AuditEventSearchPage secondPage = adapter.search(emptyQuery(1, 2, false));

		assertThat(firstPage.rows()).hasSize(2);
		assertThat(secondPage.rows()).hasSize(1);
		assertThat(firstPage.totalElements()).isEqualTo(3);
		assertThat(secondPage.totalElements()).isEqualTo(3);
	}

	@Test
	void sortsDescendingByOccurredAtByDefault() {
		AuditEventSearchPage page = adapter.search(emptyQuery(0, 20, false));

		assertThat(page.rows()).extracting(AuditEventRow::occurredAt)
				.isSortedAccordingTo((a, b) -> b.compareTo(a));
	}

	@Test
	void sortsAscendingByOccurredAtWhenRequested() {
		AuditEventSearchPage page = adapter.search(emptyQuery(0, 20, true));

		assertThat(page.rows()).extracting(AuditEventRow::occurredAt)
				.isSortedAccordingTo(Instant::compareTo);
	}

	@Test
	void returnsEmptyPageWhenNothingMatches() {
		AuditEventSearchQuery query = new AuditEventSearchQuery("UNKNOWN_TYPE", null, null, null, null, null, null, 0, 20, false);

		AuditEventSearchPage page = adapter.search(query);

		assertThat(page.rows()).isEmpty();
		assertThat(page.totalElements()).isZero();
	}

	@Test
	void eventDataRoundTripsAsRealJsonbThroughTheSearchPath() {
		AuditEventSearchQuery query = new AuditEventSearchQuery(null, null, null, null, "corr-1", null, null, 0, 20, false);

		AuditEventSearchPage page = adapter.search(query);

		assertThat(page.rows().get(0).eventData()).isEqualTo("{\"reason\":\"created\"}");
	}

	@Test
	void exactTimestampBoundaryIsInclusive() {
		AuditEventSearchQuery query = new AuditEventSearchQuery(null, null, null, null, null,
				Instant.parse("2026-01-31T23:59:59Z"), Instant.parse("2026-01-31T23:59:59Z"), 0, 20, false);

		AuditEventSearchPage page = adapter.search(query);

		assertThat(page.rows()).hasSize(1);
		assertThat(page.rows().get(0).eventType()).isEqualTo("CUSTOMER_REGISTERED");
	}

}
