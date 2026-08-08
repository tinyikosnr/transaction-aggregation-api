package za.co.tinyiko.transactionaggregation.architecture;

import java.util.List;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.categorisation.application.CategorisationDecision;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategorisationInput;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategoriseTransactionUseCase;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategoryNotFoundException;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategoryView;
import za.co.tinyiko.transactionaggregation.categorisation.application.GetCategoryUseCase;
import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRule;
import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRuleEngine;
import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRuleId;
import za.co.tinyiko.transactionaggregation.categorisation.domain.Direction;
import za.co.tinyiko.transactionaggregation.categorisation.domain.MatchField;
import za.co.tinyiko.transactionaggregation.categorisation.domain.MatchOperator;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategory;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategoryId;

/**
 * Same two rules as {@link CustomerArchitectureTests} and {@link MerchantArchitectureTests},
 * applied to {@code categorisation}. {@code CategorisationService} is package-private so it is
 * not importable from this package, and is therefore excluded from {@code APPLICATION_TYPES} -
 * the same way it is unreachable to any other real cross-module caller.
 */
class CategorisationArchitectureTests {

	private static final List<Class<?>> DOMAIN_TYPES = List.of(
			TransactionCategory.class,
			TransactionCategoryId.class,
			CategorisationRule.class,
			CategorisationRuleId.class,
			CategorisationRuleEngine.class,
			Direction.class,
			MatchField.class,
			MatchOperator.class
	);

	private static final List<Class<?>> APPLICATION_TYPES = List.of(
			CategoriseTransactionUseCase.class,
			CategorisationInput.class,
			CategorisationDecision.class,
			GetCategoryUseCase.class,
			CategoryView.class,
			CategoryNotFoundException.class
	);

	@Test
	void domainDoesNotReferenceFrameworkTypes() {
		DOMAIN_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(
				type, "org.springframework", "jakarta.persistence"));
	}

	@Test
	void applicationDoesNotReferencePersistence() {
		APPLICATION_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(
				type, "za.co.tinyiko.transactionaggregation.categorisation.persistence"));
	}

}
