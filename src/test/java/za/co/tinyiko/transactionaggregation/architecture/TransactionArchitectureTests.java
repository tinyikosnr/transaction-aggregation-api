package za.co.tinyiko.transactionaggregation.architecture;

import java.util.List;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.transaction.application.CategoryTransactionTotal;
import za.co.tinyiko.transactionaggregation.transaction.application.CreateTransactionCommand;
import za.co.tinyiko.transactionaggregation.transaction.application.CreateTransactionUseCase;
import za.co.tinyiko.transactionaggregation.transaction.application.CustomerNotFoundException;
import za.co.tinyiko.transactionaggregation.transaction.application.CustomerTransactionTotals;
import za.co.tinyiko.transactionaggregation.transaction.application.DuplicateTransactionException;
import za.co.tinyiko.transactionaggregation.transaction.application.MerchantTransactionTotal;
import za.co.tinyiko.transactionaggregation.transaction.application.MonthlyTransactionTotal;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionCreatedResult;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionQueryPort;
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
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionRepositoryPort;
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
			MonthlyTransactionTotal.class
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
			MonthlyTotalsRow.class
	);

	@Test
	void domainDoesNotReferenceFrameworkTypes() {
		DOMAIN_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(
				type, "org.springframework", "jakarta.persistence"));
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

}
