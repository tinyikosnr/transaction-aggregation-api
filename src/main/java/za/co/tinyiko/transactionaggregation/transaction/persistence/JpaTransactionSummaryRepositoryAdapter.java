package za.co.tinyiko.transactionaggregation.transaction.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import za.co.tinyiko.transactionaggregation.transaction.port.CategoryTotalsRow;
import za.co.tinyiko.transactionaggregation.transaction.port.CustomerTotalsRow;
import za.co.tinyiko.transactionaggregation.transaction.port.MerchantTotalsRow;
import za.co.tinyiko.transactionaggregation.transaction.port.MonthlyTotalsRow;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSummaryRepositoryPort;

@Component
class JpaTransactionSummaryRepositoryAdapter implements TransactionSummaryRepositoryPort {

	private final SpringDataTransactionSummaryRepository springDataTransactionSummaryRepository;

	JpaTransactionSummaryRepositoryAdapter(SpringDataTransactionSummaryRepository springDataTransactionSummaryRepository) {
		this.springDataTransactionSummaryRepository = springDataTransactionSummaryRepository;
	}

	@Override
	public CustomerTotalsRow customerTotals(UUID customerId, Instant fromInclusive, Instant toExclusive) {
		return springDataTransactionSummaryRepository.customerTotals(customerId, fromInclusive, toExclusive);
	}

	@Override
	public List<CategoryTotalsRow> categoryTotals(UUID customerId, Instant fromInclusive, Instant toExclusive) {
		return springDataTransactionSummaryRepository.categoryTotals(customerId, fromInclusive, toExclusive);
	}

	@Override
	public List<MerchantTotalsRow> merchantTotals(UUID customerId, Instant fromInclusive, Instant toExclusive) {
		return springDataTransactionSummaryRepository.merchantTotals(customerId, fromInclusive, toExclusive);
	}

	@Override
	public List<MonthlyTotalsRow> monthlyTotals(UUID customerId, Instant fromInclusive, Instant toExclusive) {
		return springDataTransactionSummaryRepository.monthlyTotalsRaw(customerId, fromInclusive, toExclusive).stream()
				.map(this::toMonthlyTotalsRow)
				.toList();
	}

	private MonthlyTotalsRow toMonthlyTotalsRow(Object[] row) {
		YearMonth month = YearMonth.from(toInstant(row[0]).atZone(ZoneOffset.UTC));
		BigDecimal totalIncome = (BigDecimal) row[1];
		BigDecimal totalExpenditure = (BigDecimal) row[2];
		long transactionCount = ((Number) row[3]).longValue();
		return new MonthlyTotalsRow(month, totalIncome, totalExpenditure, transactionCount);
	}

	private static Instant toInstant(Object value) {
		// The native query converts to UTC explicitly (`AT TIME ZONE 'UTC'`), which turns the
		// timestamptz into a timezone-less timestamp - PostgreSQL/JDBC returns that as a
		// LocalDateTime whose wall-clock value already *is* UTC, confirmed empirically
		// (verified against real PostgreSQL rather than assumed from the SQL type alone).
		if (value instanceof java.time.LocalDateTime localDateTime) {
			return localDateTime.toInstant(ZoneOffset.UTC);
		}
		if (value instanceof Instant instant) {
			return instant;
		}
		if (value instanceof java.sql.Timestamp timestamp) {
			return timestamp.toInstant();
		}
		if (value instanceof java.time.OffsetDateTime offsetDateTime) {
			return offsetDateTime.toInstant();
		}
		throw new IllegalStateException("Unexpected month_start column type: " + value.getClass());
	}

}
