package za.co.tinyiko.transactionaggregation.architecture;

import java.util.List;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.transaction.application.BulkCreateTransactionsCommand;
import za.co.tinyiko.transactionaggregation.transaction.application.BulkTransactionItemInput;
import za.co.tinyiko.transactionaggregation.transaction.application.BulkTransactionItemResult;
import za.co.tinyiko.transactionaggregation.transaction.application.BulkTransactionResult;
import za.co.tinyiko.transactionaggregation.transaction.application.CategoryTransactionTotal;
import za.co.tinyiko.transactionaggregation.transaction.application.CreateTransactionCommand;
import za.co.tinyiko.transactionaggregation.transaction.application.CreateTransactionUseCase;
import za.co.tinyiko.transactionaggregation.transaction.application.CreateTransactionsBulkUseCase;
import za.co.tinyiko.transactionaggregation.transaction.application.CustomerNotFoundException;
import za.co.tinyiko.transactionaggregation.transaction.application.CustomerTransactionTotals;
import za.co.tinyiko.transactionaggregation.transaction.application.DuplicateTransactionException;
import za.co.tinyiko.transactionaggregation.transaction.application.GetTransactionUseCase;
import za.co.tinyiko.transactionaggregation.transaction.application.MerchantTransactionTotal;
import za.co.tinyiko.transactionaggregation.transaction.application.MonthlyTransactionTotal;
import za.co.tinyiko.transactionaggregation.transaction.application.PagedResult;
import za.co.tinyiko.transactionaggregation.transaction.application.SearchTransactionsUseCase;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionCreatedResult;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionDetails;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionNotFoundException;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionQueryPort;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionSearchCriteria;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionSearchResultItem;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionSourceNotFoundException;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionValidationException;
import za.co.tinyiko.transactionaggregation.transaction.domain.Money;
import za.co.tinyiko.transactionaggregation.transaction.domain.SourceStatus;
import za.co.tinyiko.transactionaggregation.transaction.domain.Transaction;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionDirection;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionId;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionSource;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionSourceId;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionStatus;
import za.co.tinyiko.transactionaggregation.transaction.port.CategoryTotalsRow;
import za.co.tinyiko.transactionaggregation.transaction.port.CustomerTotalsRow;
import za.co.tinyiko.transactionaggregation.transaction.port.MerchantTotalsRow;
import za.co.tinyiko.transactionaggregation.transaction.port.MonthlyTotalsRow;
import za.co.tinyiko.transactionaggregation.transaction.port.RejectionReason;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionDetailRow;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionMetricsPort;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionRepositoryPort;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSearchPage;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSearchQuery;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSearchRepositoryPort;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSearchRow;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSourceRepositoryPort;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSummaryRepositoryPort;

/**
 * Same two rules as every other module's architecture tests, applied to {@code transaction},
 * plus a third checking the port layer's own dependency direction.
 * {@code CreateTransactionService}/{@code TransactionQueryService} are package-private so are
 * not importable from this package, and are therefore excluded from {@code APPLICATION_TYPES}.
 */
class TransactionArchitectureTests {

	private static final List<Class<?>> DOMAIN_TYPES = List.of(
			Transaction.class,
			TransactionId.class,
			TransactionSource.class,
			TransactionSourceId.class,
			Money.class,
			TransactionDirection.class,
			TransactionStatus.class,
			SourceStatus.class
	);

	private static final List<Class<?>> APPLICATION_TYPES = List.of(
			CreateTransactionUseCase.class,
			CreateTransactionCommand.class,
			TransactionCreatedResult.class,
			TransactionValidationException.class,
			TransactionSourceNotFoundException.class,
			CustomerNotFoundException.class,
			DuplicateTransactionException.class,
			TransactionQueryPort.class,
			CustomerTransactionTotals.class,
			CategoryTransactionTotal.class,
			MerchantTransactionTotal.class,
			MonthlyTransactionTotal.class,
			GetTransactionUseCase.class,
			TransactionDetails.class,
			TransactionNotFoundException.class,
			SearchTransactionsUseCase.class,
			TransactionSearchCriteria.class,
			TransactionSearchResultItem.class,
			PagedResult.class,
			CreateTransactionsBulkUseCase.class,
			BulkCreateTransactionsCommand.class,
			BulkTransactionItemInput.class,
			BulkTransactionItemResult.class,
			BulkTransactionResult.class
	);

	/**
	 * The dependency direction within a module must stay {@code application -> port ->
	 * persistence}, never the reverse. This is a regression test for a real mistake caught
	 * during review: {@code TransactionSummaryRepositoryPort} was originally drafted to return
	 * the public {@code transaction.application} result records directly, which would have made
	 * {@code transaction.port} depend on {@code transaction.application} - backwards. The fix
	 * was giving the port its own internal, primitive-based row types
	 * ({@code CustomerTotalsRow} etc.), with {@code TransactionQueryService} (in
	 * {@code transaction.application}) mapping between the two. This test would have caught the
	 * original mistake.
	 */
	private static final List<Class<?>> PORT_TYPES = List.of(
			TransactionRepositoryPort.class,
			TransactionSourceRepositoryPort.class,
			TransactionSummaryRepositoryPort.class,
			CustomerTotalsRow.class,
			CategoryTotalsRow.class,
			MerchantTotalsRow.class,
			MonthlyTotalsRow.class,
			TransactionSearchRepositoryPort.class,
			TransactionDetailRow.class,
			TransactionSearchRow.class,
			TransactionSearchQuery.class,
			TransactionSearchPage.class,
			TransactionMetricsPort.class,
			RejectionReason.class
	);

	@Test
	void domainDoesNotReferenceFrameworkTypes() {
		DOMAIN_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(
				type, "org.springframework", "jakarta.persistence", "io.micrometer", "org.slf4j"));
	}

	@Test
	void applicationDoesNotReferencePersistence() {
		APPLICATION_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(
				type, "za.co.tinyiko.transactionaggregation.transaction.persistence"));
	}

	@Test
	void portDoesNotReferenceApplication() {
		PORT_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(
				type, "za.co.tinyiko.transactionaggregation.transaction.application"));
	}

	/**
	 * {@code TransactionMetricsPort} itself (feature/observability) must expose no Micrometer
	 * type through its method signatures - {@code ProcessingTimer} is a framework-independent
	 * nested interface, not {@code Timer.Sample}. {@code MicrometerTransactionMetrics} (the sole
	 * implementation, in {@code transaction.metrics}) is package-private and therefore not
	 * reflectable from this test package - the same limitation already documented for every other
	 * package-private {@code @Service}/adapter in this codebase (e.g. {@code CreateTransactionService}
	 * itself is not in {@code APPLICATION_TYPES} either); its own source was written to import
	 * {@code io.micrometer.*} nowhere else in this module. {@code org.slf4j} is checked alongside
	 * it since feature/structured-logging: this module's own application/port layers must stay as
	 * free of a logging facade as they already are of Micrometer - the two approved logging call
	 * sites (feature/structured-logging) live in {@code config}/{@code api.advice}, neither of
	 * which is in {@code transaction} at all.
	 */
	@Test
	void applicationAndPortDoNotReferenceMicrometerOrSlf4j() {
		APPLICATION_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(type, "io.micrometer", "org.slf4j"));
		PORT_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(type, "io.micrometer", "org.slf4j"));
	}

}
