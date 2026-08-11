package za.co.tinyiko.transactionaggregation.architecture;

import java.util.List;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.categorisation.application.CategorisationDecision;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategorisationInput;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategorisationRuleView;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategoriseTransactionUseCase;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategoryAdminView;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategoryNotFoundException;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategoryView;
import za.co.tinyiko.transactionaggregation.categorisation.application.CreateCategorisationRuleCommand;
import za.co.tinyiko.transactionaggregation.categorisation.application.CreateCategorisationRuleUseCase;
import za.co.tinyiko.transactionaggregation.categorisation.application.GetCategorisationRuleUseCase;
import za.co.tinyiko.transactionaggregation.categorisation.application.GetCategoryUseCase;
import za.co.tinyiko.transactionaggregation.categorisation.application.ListCategoriesUseCase;
import za.co.tinyiko.transactionaggregation.categorisation.application.ListCategorisationRulesUseCase;
import za.co.tinyiko.transactionaggregation.categorisation.application.RuleConflictException;
import za.co.tinyiko.transactionaggregation.categorisation.application.RuleNotFoundException;
import za.co.tinyiko.transactionaggregation.categorisation.application.RuleValidationException;
import za.co.tinyiko.transactionaggregation.categorisation.application.UpdateCategorisationRuleCommand;
import za.co.tinyiko.transactionaggregation.categorisation.application.UpdateCategorisationRuleUseCase;
import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRule;
import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRuleEngine;
import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRuleId;
import za.co.tinyiko.transactionaggregation.categorisation.domain.Direction;
import za.co.tinyiko.transactionaggregation.categorisation.domain.MatchField;
import za.co.tinyiko.transactionaggregation.categorisation.domain.MatchOperator;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategory;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategoryId;
import za.co.tinyiko.transactionaggregation.categorisation.port.ActiveCategorisationRule;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategorisationRuleRepositoryPort;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategorisationRuleRow;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategoryRepositoryPort;

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
			CategoryNotFoundException.class,
			ListCategoriesUseCase.class,
			CategoryAdminView.class,
			ListCategorisationRulesUseCase.class,
			GetCategorisationRuleUseCase.class,
			CategorisationRuleView.class,
			CreateCategorisationRuleUseCase.class,
			CreateCategorisationRuleCommand.class,
			UpdateCategorisationRuleUseCase.class,
			UpdateCategorisationRuleCommand.class,
			RuleNotFoundException.class,
			RuleConflictException.class,
			RuleValidationException.class
	);

	/**
	 * Added in {@code feature/category-admin}, the first time this port's dependency direction
	 * became worth guarding explicitly: {@link CategorisationRuleRepositoryPort#update} returns
	 * {@link CategorisationRuleRow}, this port's own row type, never an
	 * {@code application}-layer type - the same "port owns its own shape" rule
	 * {@code TransactionArchitectureTests#portDoesNotReferenceApplication} already guards for
	 * {@code transaction}.
	 */
	private static final List<Class<?>> PORT_TYPES = List.of(
			CategoryRepositoryPort.class,
			CategorisationRuleRepositoryPort.class,
			CategorisationRuleRow.class,
			ActiveCategorisationRule.class
	);

	@Test
	void domainDoesNotReferenceFrameworkTypes() {
		DOMAIN_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(
				type, "org.springframework", "jakarta.persistence", "org.slf4j"));
	}

	@Test
	void applicationDoesNotReferencePersistence() {
		APPLICATION_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(
				type, "za.co.tinyiko.transactionaggregation.categorisation.persistence"));
	}

	@Test
	void portDoesNotReferenceApplication() {
		PORT_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(
				type, "za.co.tinyiko.transactionaggregation.categorisation.application"));
	}

}
