package za.co.tinyiko.transactionaggregation.audit.application;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import za.co.tinyiko.transactionaggregation.audit.port.AuditEventRow;
import za.co.tinyiko.transactionaggregation.audit.port.AuditEventSearchPage;
import za.co.tinyiko.transactionaggregation.audit.port.AuditEventSearchQuery;
import za.co.tinyiko.transactionaggregation.audit.port.AuditQueryRepositoryPort;

/**
 * All structural validation - page/size bounds, blank-filter normalisation, the
 * "{@code aggregateId} requires {@code aggregateType}" rule, date-range ordering, and sort
 * whitelisting - lives here, matching where every other business-invariant/structural check for
 * a search use case already lives in this codebase (see
 * {@code transaction.application.SearchTransactionsService}). No {@code @Transactional}: this is
 * a pure read, and no other read/search/list service in this codebase adds one either (consistency
 * over a point-in-time optimisation with no demonstrated need).
 */
@Service
class SearchAuditEventsService implements SearchAuditEventsUseCase {

	private static final int MIN_PAGE = 0;
	private static final int MIN_SIZE = 1;
	private static final int MAX_SIZE = 100;
	private static final String ALLOWED_SORT_FIELD = "occurredAt";

	private final AuditQueryRepositoryPort auditQueryRepositoryPort;

	SearchAuditEventsService(AuditQueryRepositoryPort auditQueryRepositoryPort) {
		this.auditQueryRepositoryPort = auditQueryRepositoryPort;
	}

	@Override
	public AuditEventSearchResult search(AuditEventSearchCriteria criteria) {
		validatePageAndSize(criteria.page(), criteria.size());
		validateDateRange(criteria.occurredFrom(), criteria.occurredTo());
		boolean ascending = parseSortAscending(criteria.sort());

		String aggregateType = normalize(criteria.aggregateType());
		String eventType = normalize(criteria.eventType());
		String actor = normalize(criteria.actor());
		String correlationId = normalize(criteria.correlationId());

		if (criteria.aggregateId() != null && aggregateType == null) {
			throw new AuditSearchValidationException("aggregateType is required when aggregateId is supplied");
		}

		AuditEventSearchQuery query = new AuditEventSearchQuery(
				aggregateType, criteria.aggregateId(), eventType, actor, correlationId,
				criteria.occurredFrom(), criteria.occurredTo(), criteria.page(), criteria.size(), ascending);

		AuditEventSearchPage page = auditQueryRepositoryPort.search(query);
		List<AuditEventView> content = page.rows().stream().map(SearchAuditEventsService::toView).toList();

		int totalPages = page.totalElements() == 0 ? 0 : (int) Math.ceil((double) page.totalElements() / criteria.size());
		return new AuditEventSearchResult(content, criteria.page(), criteria.size(), page.totalElements(), totalPages);
	}

	/**
	 * {@code null}, empty, and whitespace-only all normalise to "absent" - an optional string
	 * filter must not be silently treated as a real, matchable empty-string value (feature/audit-query,
	 * an explicit project decision, not inferred framework behaviour).
	 */
	private static String normalize(String value) {
		return (value == null || value.isBlank()) ? null : value;
	}

	private static void validatePageAndSize(int page, int size) {
		if (page < MIN_PAGE) {
			throw new AuditSearchValidationException("page must not be negative");
		}
		if (size < MIN_SIZE || size > MAX_SIZE) {
			throw new AuditSearchValidationException("size must be between " + MIN_SIZE + " and " + MAX_SIZE);
		}
	}

	/**
	 * Both bounds independently optional; no maximum span is enforced - an explicit project
	 * decision (feature/audit-query), unlike transaction search's 24-month cap, since audit is a
	 * compliance query surface that may legitimately need long historical lookback.
	 */
	private static void validateDateRange(Instant occurredFrom, Instant occurredTo) {
		if (occurredFrom == null || occurredTo == null) {
			return;
		}
		if (occurredFrom.isAfter(occurredTo)) {
			throw new AuditSearchValidationException("occurredFrom must not be after occurredTo");
		}
	}

	/**
	 * Whitelist limited to exactly {@code occurredAt} - the only sortable field. Defaults to
	 * {@code occurredAt,desc} when no {@code sort} is supplied.
	 */
	private static boolean parseSortAscending(String sort) {
		if (sort == null || sort.isBlank()) {
			return false;
		}
		String[] parts = sort.split(",", 2);
		String field = parts[0].trim();
		if (!ALLOWED_SORT_FIELD.equals(field)) {
			throw new AuditSearchValidationException("unsupported sort field: " + field);
		}
		String sortDirection = parts.length > 1 ? parts[1].trim() : "desc";
		if (sortDirection.equalsIgnoreCase("asc")) {
			return true;
		}
		if (sortDirection.equalsIgnoreCase("desc")) {
			return false;
		}
		throw new AuditSearchValidationException("unsupported sort direction: " + sortDirection);
	}

	private static AuditEventView toView(AuditEventRow row) {
		return new AuditEventView(
				row.id(), row.aggregateType(), row.aggregateId(), row.eventType(),
				row.actor(), row.correlationId(), row.eventData(), row.occurredAt());
	}

}
