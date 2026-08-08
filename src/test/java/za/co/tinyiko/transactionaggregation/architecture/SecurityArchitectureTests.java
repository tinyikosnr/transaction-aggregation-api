package za.co.tinyiko.transactionaggregation.architecture;

import java.util.List;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.aggregation.application.CategorySummaryView;
import za.co.tinyiko.transactionaggregation.aggregation.application.CustomerSummaryView;
import za.co.tinyiko.transactionaggregation.aggregation.application.GetCategorySummaryUseCase;
import za.co.tinyiko.transactionaggregation.aggregation.application.GetCustomerSummaryUseCase;
import za.co.tinyiko.transactionaggregation.aggregation.application.GetMerchantSummaryUseCase;
import za.co.tinyiko.transactionaggregation.aggregation.application.GetMonthlySummaryUseCase;
import za.co.tinyiko.transactionaggregation.aggregation.application.MerchantSummaryView;
import za.co.tinyiko.transactionaggregation.aggregation.application.MonthlySummaryView;
import za.co.tinyiko.transactionaggregation.aggregation.domain.DateRange;
import za.co.tinyiko.transactionaggregation.audit.application.RecordAuditEventCommand;
import za.co.tinyiko.transactionaggregation.audit.application.RecordAuditEventUseCase;
import za.co.tinyiko.transactionaggregation.audit.domain.AuditEvent;
import za.co.tinyiko.transactionaggregation.audit.domain.AuditEventId;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategorisationDecision;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategorisationInput;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategoriseTransactionUseCase;
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
import za.co.tinyiko.transactionaggregation.customer.application.CustomerExistsPort;
import za.co.tinyiko.transactionaggregation.customer.application.CustomerLookupPort;
import za.co.tinyiko.transactionaggregation.customer.application.DuplicateCustomerReferenceException;
import za.co.tinyiko.transactionaggregation.customer.application.RegisterCustomerCommand;
import za.co.tinyiko.transactionaggregation.customer.application.RegisterCustomerUseCase;
import za.co.tinyiko.transactionaggregation.customer.domain.Customer;
import za.co.tinyiko.transactionaggregation.customer.domain.CustomerId;
import za.co.tinyiko.transactionaggregation.customer.domain.CustomerStatus;
import za.co.tinyiko.transactionaggregation.merchant.application.DuplicateMerchantException;
import za.co.tinyiko.transactionaggregation.merchant.application.MerchantResolutionPort;
import za.co.tinyiko.transactionaggregation.merchant.domain.Merchant;
import za.co.tinyiko.transactionaggregation.merchant.domain.MerchantId;
import za.co.tinyiko.transactionaggregation.merchant.domain.MerchantNormaliser;
import za.co.tinyiko.transactionaggregation.security.SecurityConfig;
import za.co.tinyiko.transactionaggregation.transaction.application.CreateTransactionCommand;
import za.co.tinyiko.transactionaggregation.transaction.application.CreateTransactionUseCase;
import za.co.tinyiko.transactionaggregation.transaction.application.CustomerTransactionTotals;
import za.co.tinyiko.transactionaggregation.transaction.application.DuplicateTransactionException;
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

/**
 * Enforces the boundary this branch introduces: no business module depends on {@code security},
 * in either direction. Spring Modulith's own {@code verify()} ({@link ModularityTests}) already
 * covers this generically once {@code security}'s {@code @ApplicationModule(allowedDependencies =
 * "shared")} is in place - a business module importing {@code security} would fail that check
 * too - but this test makes the specific, intended rule explicit and independently verifiable,
 * the same way {@code TransactionArchitectureTests#portDoesNotReferenceApplication} exists
 * alongside {@code verify()} rather than instead of it.
 *
 * <p>Reuses every populated business module's own {@code DOMAIN_TYPES}/{@code APPLICATION_TYPES}
 * class lists (duplicated here rather than shared, since those lists are private to each
 * module's own architecture test) rather than introducing a new mechanism.
 *
 * <p>{@code security.SecurityConfig} is the only public type in {@code security} - its
 * package-private collaborators ({@code RoleClaimAuthoritiesConverter},
 * {@code ProblemDetailAuthenticationEntryPoint}, {@code ProblemDetailAccessDeniedHandler},
 * {@code ProblemDetailSupport}) are not importable from this package, the same limitation already
 * documented for other modules' package-private services (e.g. {@code CreateTransactionService}
 * is never reflectively checked, only {@code CreateTransactionUseCase}).
 */
class SecurityArchitectureTests {

	private static final List<Class<?>> BUSINESS_MODULE_TYPES = List.of(
			// transaction
			Transaction.class, TransactionId.class, TransactionSource.class, TransactionSourceId.class,
			Money.class, TransactionDirection.class, TransactionStatus.class, SourceStatus.class,
			CreateTransactionUseCase.class, CreateTransactionCommand.class, TransactionCreatedResult.class,
			TransactionValidationException.class, TransactionSourceNotFoundException.class,
			za.co.tinyiko.transactionaggregation.transaction.application.CustomerNotFoundException.class,
			DuplicateTransactionException.class, TransactionQueryPort.class, CustomerTransactionTotals.class,
			// categorisation
			TransactionCategory.class, TransactionCategoryId.class, CategorisationRule.class,
			CategorisationRuleId.class, CategorisationRuleEngine.class, Direction.class, MatchField.class,
			MatchOperator.class, CategoriseTransactionUseCase.class, CategorisationInput.class,
			CategorisationDecision.class, GetCategoryUseCase.class, CategoryView.class,
			za.co.tinyiko.transactionaggregation.categorisation.application.CategoryNotFoundException.class,
			// aggregation
			DateRange.class, GetCustomerSummaryUseCase.class, GetCategorySummaryUseCase.class,
			GetMerchantSummaryUseCase.class, GetMonthlySummaryUseCase.class, CustomerSummaryView.class,
			CategorySummaryView.class, MerchantSummaryView.class, MonthlySummaryView.class,
			za.co.tinyiko.transactionaggregation.aggregation.application.CustomerNotFoundException.class,
			// customer
			Customer.class, CustomerId.class, CustomerStatus.class, CustomerLookupPort.class,
			CustomerExistsPort.class, RegisterCustomerUseCase.class, RegisterCustomerCommand.class,
			za.co.tinyiko.transactionaggregation.customer.application.CustomerNotFoundException.class,
			DuplicateCustomerReferenceException.class,
			// merchant
			Merchant.class, MerchantId.class, MerchantNormaliser.class, MerchantResolutionPort.class,
			DuplicateMerchantException.class,
			// audit
			AuditEvent.class, AuditEventId.class, RecordAuditEventUseCase.class, RecordAuditEventCommand.class
	);

	@Test
	void noBusinessModuleTypeReferencesSpringSecurityOrTheSecurityModule() {
		BUSINESS_MODULE_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(
				type, "org.springframework.security", "za.co.tinyiko.transactionaggregation.security"));
	}

	@Test
	void securityConfigDoesNotReferenceAnyBusinessModulesDomainPersistenceOrApplicationPackage() {
		FrameworkIndependenceAssertions.assertNoForbiddenReference(SecurityConfig.class,
				"za.co.tinyiko.transactionaggregation.transaction.domain",
				"za.co.tinyiko.transactionaggregation.transaction.persistence",
				"za.co.tinyiko.transactionaggregation.transaction.application",
				"za.co.tinyiko.transactionaggregation.categorisation.domain",
				"za.co.tinyiko.transactionaggregation.categorisation.persistence",
				"za.co.tinyiko.transactionaggregation.categorisation.application",
				"za.co.tinyiko.transactionaggregation.aggregation.domain",
				"za.co.tinyiko.transactionaggregation.aggregation.application",
				"za.co.tinyiko.transactionaggregation.customer.domain",
				"za.co.tinyiko.transactionaggregation.customer.persistence",
				"za.co.tinyiko.transactionaggregation.customer.application",
				"za.co.tinyiko.transactionaggregation.merchant.domain",
				"za.co.tinyiko.transactionaggregation.merchant.persistence",
				"za.co.tinyiko.transactionaggregation.merchant.application",
				"za.co.tinyiko.transactionaggregation.audit.domain",
				"za.co.tinyiko.transactionaggregation.audit.persistence",
				"za.co.tinyiko.transactionaggregation.audit.application");
	}

}
