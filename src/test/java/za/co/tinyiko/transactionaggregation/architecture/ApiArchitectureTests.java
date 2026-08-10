package za.co.tinyiko.transactionaggregation.architecture;

import java.util.List;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.api.dto.request.BulkCreateTransactionsRequest;
import za.co.tinyiko.transactionaggregation.api.dto.request.CreateCategorisationRuleRequest;
import za.co.tinyiko.transactionaggregation.api.dto.request.CreateTransactionRequest;
import za.co.tinyiko.transactionaggregation.api.dto.request.UpdateCategorisationRuleRequest;
import za.co.tinyiko.transactionaggregation.api.dto.response.BulkTransactionItemResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.BulkTransactionResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.CategorisationRuleResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.CategoryResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.CategorySummaryResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.CustomerSummaryResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.MerchantSummaryResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.MonthlySummaryResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.TransactionResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.TransactionSearchItemResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.TransactionSearchResponse;
import za.co.tinyiko.transactionaggregation.api.mapper.AggregationApiMapper;
import za.co.tinyiko.transactionaggregation.api.mapper.CategoryAdminApiMapper;
import za.co.tinyiko.transactionaggregation.api.mapper.TransactionApiMapper;

/**
 * Layering checks for {@code api}, the presentation layer. {@code api.controller} and
 * {@code api.advice} classes are package-private (matching every module's own {@code @Service}
 * convention), so - like {@code CreateTransactionService}/{@code CategorisationService} in their
 * own modules' architecture tests - they are not importable from this test package and are
 * therefore not covered here directly; their behaviour is instead exercised by
 * {@code TransactionControllerTests}/{@code AggregationControllerTests}/
 * {@code GlobalExceptionHandlerTests}. What reflection over the public {@code api.dto}/
 * {@code api.mapper} types already lets us check for free, we do check.
 */
class ApiArchitectureTests {

	private static final List<Class<?>> REQUEST_DTO_TYPES = List.of(
			CreateTransactionRequest.class,
			BulkCreateTransactionsRequest.class,
			CreateCategorisationRuleRequest.class,
			UpdateCategorisationRuleRequest.class
	);

	private static final List<Class<?>> RESPONSE_DTO_TYPES = List.of(
			TransactionResponse.class,
			TransactionResponse.MerchantInfo.class,
			TransactionResponse.CategoryInfo.class,
			CustomerSummaryResponse.class,
			CustomerSummaryResponse.Period.class,
			CustomerSummaryResponse.MoneyAmount.class,
			CategorySummaryResponse.class,
			MerchantSummaryResponse.class,
			MonthlySummaryResponse.class,
			TransactionSearchItemResponse.class,
			TransactionSearchResponse.class,
			TransactionSearchResponse.PageInfo.class,
			BulkTransactionResponse.class,
			BulkTransactionItemResponse.class,
			CategoryResponse.class,
			CategorisationRuleResponse.class
	);

	private static final List<Class<?>> MAPPER_TYPES = List.of(
			TransactionApiMapper.class,
			AggregationApiMapper.class,
			CategoryAdminApiMapper.class
	);

	/**
	 * {@code jakarta.validation} is deliberately not in the forbidden list here - Bean
	 * Validation annotations on a request DTO are the one framework dependency this layer is
	 * expected to carry (CLAUDE.md: "DTO-level validation | Jakarta Bean Validation annotations
	 * on api.request types").
	 */
	@Test
	void requestDtosDoNotReferenceDomainOrPersistenceTypes() {
		REQUEST_DTO_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(
				type,
				"org.springframework",
				"jakarta.persistence",
				"za.co.tinyiko.transactionaggregation.transaction.domain",
				"za.co.tinyiko.transactionaggregation.transaction.persistence"));
	}

	@Test
	void responseDtosDoNotReferenceFrameworkOrDomainTypes() {
		RESPONSE_DTO_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(
				type,
				"org.springframework",
				"jakarta.persistence",
				"jakarta.validation",
				"za.co.tinyiko.transactionaggregation.transaction.domain",
				"za.co.tinyiko.transactionaggregation.transaction.persistence",
				"za.co.tinyiko.transactionaggregation.aggregation.domain",
				"za.co.tinyiko.transactionaggregation.categorisation.domain",
				"za.co.tinyiko.transactionaggregation.categorisation.persistence"));
	}

	/**
	 * Mappers translate {@code api.dto} shapes to/from each module's own {@code application}
	 * contracts (allowed), never a module's {@code domain} or {@code persistence} types directly
	 * - a mapper reaching past the application layer would be exactly the kind of boundary leak
	 * {@code TransactionArchitectureTests#portDoesNotReferenceApplication} already guards against
	 * for {@code transaction} internally; this is the equivalent guard at the presentation edge.
	 */
	@Test
	void mappersDoNotReferenceDomainOrPersistenceOfAnyModule() {
		MAPPER_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(
				type,
				"za.co.tinyiko.transactionaggregation.transaction.domain",
				"za.co.tinyiko.transactionaggregation.transaction.persistence",
				"za.co.tinyiko.transactionaggregation.aggregation.domain",
				"za.co.tinyiko.transactionaggregation.aggregation.persistence",
				"za.co.tinyiko.transactionaggregation.categorisation.domain",
				"za.co.tinyiko.transactionaggregation.categorisation.persistence"));
	}

}
