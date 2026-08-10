package za.co.tinyiko.transactionaggregation.transaction.application;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import za.co.tinyiko.transactionaggregation.categorisation.application.CategoryView;
import za.co.tinyiko.transactionaggregation.categorisation.application.GetCategoryUseCase;
import za.co.tinyiko.transactionaggregation.merchant.application.GetMerchantsUseCase;
import za.co.tinyiko.transactionaggregation.merchant.application.MerchantView;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionDirection;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionStatus;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSearchPage;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSearchQuery;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSearchRepositoryPort;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSearchRow;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSourceRepositoryPort;

/**
 * All structural validation (page/size bounds, date-range, direction/status, sort whitelist) and
 * both code-to-id filter resolutions live here, matching where every other
 * business-invariant/structural check for this module already lives.
 *
 * <p>Enrichment is batched, not per-row: the distinct, non-null merchant/category ids across the
 * current page only are collected once, then resolved via exactly one
 * {@link GetMerchantsUseCase#findByIds} call and one {@link GetCategoryUseCase#getByIds} call -
 * three queries total per search call (one page query, two batch lookups), independent of page
 * size.
 */
@Service
class SearchTransactionsService implements SearchTransactionsUseCase {

	private static final int MIN_PAGE = 0;
	private static final int MIN_SIZE = 1;
	private static final int MAX_SIZE = 100;
	private static final long MAX_RANGE_MONTHS = 24;
	private static final String ALLOWED_SORT_FIELD = "transactionTimestamp";

	private final TransactionSearchRepositoryPort transactionSearchRepositoryPort;
	private final TransactionSourceRepositoryPort transactionSourceRepositoryPort;
	private final GetCategoryUseCase getCategoryUseCase;
	private final GetMerchantsUseCase getMerchantsUseCase;

	SearchTransactionsService(
			TransactionSearchRepositoryPort transactionSearchRepositoryPort,
			TransactionSourceRepositoryPort transactionSourceRepositoryPort,
			GetCategoryUseCase getCategoryUseCase,
			GetMerchantsUseCase getMerchantsUseCase
	) {
		this.transactionSearchRepositoryPort = transactionSearchRepositoryPort;
		this.transactionSourceRepositoryPort = transactionSourceRepositoryPort;
		this.getCategoryUseCase = getCategoryUseCase;
		this.getMerchantsUseCase = getMerchantsUseCase;
	}

	@Override
	public PagedResult<TransactionSearchResultItem> search(TransactionSearchCriteria criteria) {
		validatePageAndSize(criteria.page(), criteria.size());
		validateDateRange(criteria.occurredFrom(), criteria.occurredTo());
		String direction = parseDirection(criteria.direction());
		String status = parseStatus(criteria.status());
		boolean ascending = parseSortAscending(criteria.sort());

		UUID transactionSourceId = resolveSourceId(criteria.sourceCode());
		UUID categoryId = resolveCategoryId(criteria.categoryCode());

		TransactionSearchQuery query = new TransactionSearchQuery(
				criteria.customerId(), transactionSourceId, categoryId, criteria.merchantId(),
				direction, status, criteria.occurredFrom(), criteria.occurredTo(),
				criteria.page(), criteria.size(), ascending);

		TransactionSearchPage page = transactionSearchRepositoryPort.search(query);
		List<TransactionSearchResultItem> items = enrich(page.rows());

		int totalPages = page.totalElements() == 0 ? 0 : (int) Math.ceil((double) page.totalElements() / criteria.size());
		return new PagedResult<>(items, criteria.page(), criteria.size(), page.totalElements(), totalPages);
	}

	private List<TransactionSearchResultItem> enrich(List<TransactionSearchRow> rows) {
		Set<UUID> merchantIds = rows.stream().map(TransactionSearchRow::merchantId).filter(Objects::nonNull).collect(Collectors.toSet());
		Set<UUID> categoryIds = rows.stream().map(TransactionSearchRow::categoryId).filter(Objects::nonNull).collect(Collectors.toSet());

		Map<UUID, String> merchantNames = merchantIds.isEmpty() ? Map.of() : getMerchantsUseCase.findByIds(merchantIds).stream()
				.collect(Collectors.toMap(MerchantView::merchantId, MerchantView::displayName));
		Map<UUID, String> categoryCodes = categoryIds.isEmpty() ? Map.of() : getCategoryUseCase.getByIds(categoryIds).stream()
				.collect(Collectors.toMap(CategoryView::categoryId, CategoryView::code));

		return rows.stream()
				.map(row -> new TransactionSearchResultItem(
						row.id(),
						row.customerId(),
						row.merchantId() == null ? null : merchantNames.get(row.merchantId()),
						categoryCodes.get(row.categoryId()),
						row.amount(),
						row.currency(),
						row.direction(),
						row.occurredAt()))
				.toList();
	}

	private static void validatePageAndSize(int page, int size) {
		if (page < MIN_PAGE) {
			throw new TransactionValidationException("page must not be negative", null);
		}
		if (size < MIN_SIZE || size > MAX_SIZE) {
			throw new TransactionValidationException("size must be between " + MIN_SIZE + " and " + MAX_SIZE, null);
		}
	}

	/**
	 * Both bounds independently optional; the 24-month limit (TDS §31) is only checked when both
	 * are supplied, since an open-ended range has no defined span to validate.
	 */
	private static void validateDateRange(Instant occurredFrom, Instant occurredTo) {
		if (occurredFrom == null || occurredTo == null) {
			return;
		}
		if (occurredFrom.isAfter(occurredTo)) {
			throw new TransactionValidationException("occurredFrom must not be after occurredTo", null);
		}
		long monthsBetween = ChronoUnit.MONTHS.between(occurredFrom.atZone(ZoneOffset.UTC), occurredTo.atZone(ZoneOffset.UTC));
		if (monthsBetween > MAX_RANGE_MONTHS) {
			throw new TransactionValidationException("date range must not exceed " + MAX_RANGE_MONTHS + " months", null);
		}
	}

	private static String parseDirection(String direction) {
		if (direction == null) {
			return null;
		}
		try {
			return TransactionDirection.valueOf(direction).name();
		} catch (IllegalArgumentException e) {
			throw new TransactionValidationException("direction must be CREDIT or DEBIT", e);
		}
	}

	private static String parseStatus(String status) {
		if (status == null) {
			return null;
		}
		try {
			return TransactionStatus.valueOf(status).name();
		} catch (IllegalArgumentException e) {
			throw new TransactionValidationException("status must be one of RECEIVED, PROCESSED, REJECTED", e);
		}
	}

	/**
	 * Whitelist limited to exactly {@code transactionTimestamp -> occurredAt} - the only sortable
	 * field the documentation establishes (via TDS §31's own default example). Returns whether
	 * the resolved direction is ascending; defaults to {@code transactionTimestamp,desc} (TDS
	 * §31's documented default) when no {@code sort} is supplied.
	 */
	private static boolean parseSortAscending(String sort) {
		if (sort == null || sort.isBlank()) {
			return false;
		}
		String[] parts = sort.split(",", 2);
		String field = parts[0].trim();
		if (!ALLOWED_SORT_FIELD.equals(field)) {
			throw new TransactionValidationException("unsupported sort field: " + field, null);
		}
		String sortDirection = parts.length > 1 ? parts[1].trim() : "desc";
		if (sortDirection.equalsIgnoreCase("asc")) {
			return true;
		}
		if (sortDirection.equalsIgnoreCase("desc")) {
			return false;
		}
		throw new TransactionValidationException("unsupported sort direction: " + sortDirection, null);
	}

	/**
	 * A {@code sourceCode} that doesn't resolve to a real source must yield zero search results,
	 * not "no filter applied" - a random, guaranteed-non-matching id achieves that without a
	 * separate empty-result short-circuit path.
	 */
	private UUID resolveSourceId(String sourceCode) {
		if (sourceCode == null) {
			return null;
		}
		return transactionSourceRepositoryPort.findByCode(sourceCode)
				.map(source -> source.id().value())
				.orElseGet(UUID::randomUUID);
	}

	private UUID resolveCategoryId(String categoryCode) {
		if (categoryCode == null) {
			return null;
		}
		return getCategoryUseCase.findIdByCode(categoryCode)
				.orElseGet(UUID::randomUUID);
	}

}
