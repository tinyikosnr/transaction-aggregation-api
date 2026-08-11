package za.co.tinyiko.transactionaggregation.architecture;

import java.util.List;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.aggregation.application.CategorySummaryView;
import za.co.tinyiko.transactionaggregation.aggregation.application.CustomerNotFoundException;
import za.co.tinyiko.transactionaggregation.aggregation.application.CustomerSummaryView;
import za.co.tinyiko.transactionaggregation.aggregation.application.GetCategorySummaryUseCase;
import za.co.tinyiko.transactionaggregation.aggregation.application.GetCustomerSummaryUseCase;
import za.co.tinyiko.transactionaggregation.aggregation.application.GetMerchantSummaryUseCase;
import za.co.tinyiko.transactionaggregation.aggregation.application.GetMonthlySummaryUseCase;
import za.co.tinyiko.transactionaggregation.aggregation.application.MerchantSummaryView;
import za.co.tinyiko.transactionaggregation.aggregation.application.MonthlySummaryView;
import za.co.tinyiko.transactionaggregation.aggregation.domain.DateRange;

/**
 * Same two rules as every other module's architecture tests, applied to {@code aggregation}.
 * The four {@code Get*SummaryService} classes are package-private so are not importable from
 * this package, and are therefore excluded from {@code APPLICATION_TYPES}.
 *
 * <p>{@code aggregation} has no {@code persistence} package at all in this branch (SAD 31.3:
 * it owns no persistent data), so {@link #applicationDoesNotReferencePersistence()} is
 * somewhat vacuous today - kept anyway as a tripwire against one being added prematurely, and
 * for consistency with every other module's architecture test shape.
 */
class AggregationArchitectureTests {

	private static final List<Class<?>> DOMAIN_TYPES = List.of(
			DateRange.class
	);

	private static final List<Class<?>> APPLICATION_TYPES = List.of(
			GetCustomerSummaryUseCase.class,
			GetCategorySummaryUseCase.class,
			GetMerchantSummaryUseCase.class,
			GetMonthlySummaryUseCase.class,
			CustomerSummaryView.class,
			CategorySummaryView.class,
			MerchantSummaryView.class,
			MonthlySummaryView.class,
			CustomerNotFoundException.class
	);

	@Test
	void domainDoesNotReferenceFrameworkTypes() {
		DOMAIN_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(
				type, "org.springframework", "jakarta.persistence", "org.slf4j"));
	}

	@Test
	void applicationDoesNotReferencePersistence() {
		APPLICATION_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(
				type, "za.co.tinyiko.transactionaggregation.aggregation.persistence"));
	}

}
