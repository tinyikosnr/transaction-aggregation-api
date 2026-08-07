package za.co.tinyiko.transactionaggregation.architecture;

import java.util.List;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.transaction.application.CreateTransactionCommand;
import za.co.tinyiko.transactionaggregation.transaction.application.CreateTransactionUseCase;
import za.co.tinyiko.transactionaggregation.transaction.application.CustomerNotFoundException;
import za.co.tinyiko.transactionaggregation.transaction.application.DuplicateTransactionException;
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

/**
 * Same two rules as every other module's architecture tests, applied to {@code transaction}.
 * {@code CreateTransactionService} is package-private so it is not importable from this
 * package, and is therefore excluded from {@code APPLICATION_TYPES}.
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
			TransactionValidationException.class,
			TransactionSourceNotFoundException.class,
			CustomerNotFoundException.class,
			DuplicateTransactionException.class
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

}
