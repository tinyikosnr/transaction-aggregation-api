package za.co.tinyiko.transactionaggregation.transaction.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import za.co.tinyiko.transactionaggregation.transaction.port.CategoryTotalsRow;
import za.co.tinyiko.transactionaggregation.transaction.port.CustomerTotalsRow;
import za.co.tinyiko.transactionaggregation.transaction.port.MerchantTotalsRow;
import za.co.tinyiko.transactionaggregation.transaction.port.MonthlyTotalsRow;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSummaryRepositoryPort;

/**
 * Implements {@link TransactionQueryPort} by calling {@link TransactionSummaryRepositoryPort}
 * and mapping its internal, port-local row projections to this package's public result records.
 * This mapping step is exactly why the two shapes are kept as distinct types even though they
 * look identical field-for-field: it's what lets {@code transaction.port} avoid ever depending
 * on {@code transaction.application}, keeping the dependency direction strictly
 * {@code application -> port -> persistence}.
 *
 * <p>Also owns the {@code LocalDate} -> {@code Instant} range translation: both bounds inclusive
 * on the {@code LocalDate} side, translated to an inclusive-lower/exclusive-upper UTC instant
 * range so the full {@code to} date is included.
 */
@Service
class TransactionQueryService implements TransactionQueryPort {

	private final TransactionSummaryRepositoryPort transactionSummaryRepositoryPort;

	TransactionQueryService(TransactionSummaryRepositoryPort transactionSummaryRepositoryPort) {
		this.transactionSummaryRepositoryPort = transactionSummaryRepositoryPort;
	}

	@Override
	public CustomerTransactionTotals customerTotals(UUID customerId, LocalDate from, LocalDate to) {
		CustomerTotalsRow row = transactionSummaryRepositoryPort.customerTotals(
				customerId, toInstant(from), toExclusiveInstant(to));
		return new CustomerTransactionTotals(row.totalIncome(), row.totalExpenditure(), row.transactionCount());
	}

	@Override
	public List<CategoryTransactionTotal> categoryTotals(UUID customerId, LocalDate from, LocalDate to) {
		List<CategoryTotalsRow> rows = transactionSummaryRepositoryPort.categoryTotals(
				customerId, toInstant(from), toExclusiveInstant(to));
		return rows.stream()
				.map(row -> new CategoryTransactionTotal(row.categoryId(), row.totalAmount(), row.transactionCount()))
				.toList();
	}

	@Override
	public List<MerchantTransactionTotal> merchantTotals(UUID customerId, LocalDate from, LocalDate to) {
		List<MerchantTotalsRow> rows = transactionSummaryRepositoryPort.merchantTotals(
				customerId, toInstant(from), toExclusiveInstant(to));
		return rows.stream()
				.map(row -> new MerchantTransactionTotal(row.merchantId(), row.totalAmount(), row.transactionCount()))
				.toList();
	}

	@Override
	public List<MonthlyTransactionTotal> monthlyTotals(UUID customerId, LocalDate from, LocalDate to) {
		List<MonthlyTotalsRow> rows = transactionSummaryRepositoryPort.monthlyTotals(
				customerId, toInstant(from), toExclusiveInstant(to));
		return rows.stream()
				.map(row -> new MonthlyTransactionTotal(row.month(), row.totalIncome(), row.totalExpenditure(), row.transactionCount()))
				.toList();
	}

	private static Instant toInstant(LocalDate date) {
		return date.atStartOfDay(ZoneOffset.UTC).toInstant();
	}

	private static Instant toExclusiveInstant(LocalDate date) {
		return date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
	}

}
