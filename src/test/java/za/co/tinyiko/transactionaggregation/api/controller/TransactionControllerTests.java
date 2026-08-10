package za.co.tinyiko.transactionaggregation.api.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import tools.jackson.databind.ObjectMapper;

import za.co.tinyiko.transactionaggregation.api.dto.request.BulkCreateTransactionsRequest;
import za.co.tinyiko.transactionaggregation.api.dto.request.CreateTransactionRequest;
import za.co.tinyiko.transactionaggregation.security.SecurityConfig;
import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;
import za.co.tinyiko.transactionaggregation.transaction.application.BulkTransactionItemResult;
import za.co.tinyiko.transactionaggregation.transaction.application.BulkTransactionResult;
import za.co.tinyiko.transactionaggregation.transaction.application.CreateTransactionCommand;
import za.co.tinyiko.transactionaggregation.transaction.application.CreateTransactionUseCase;
import za.co.tinyiko.transactionaggregation.transaction.application.CreateTransactionsBulkUseCase;
import za.co.tinyiko.transactionaggregation.transaction.application.CustomerNotFoundException;
import za.co.tinyiko.transactionaggregation.transaction.application.DuplicateTransactionException;
import za.co.tinyiko.transactionaggregation.transaction.application.GetTransactionUseCase;
import za.co.tinyiko.transactionaggregation.transaction.application.PagedResult;
import za.co.tinyiko.transactionaggregation.transaction.application.SearchTransactionsUseCase;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionCreatedResult;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionDetails;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionNotFoundException;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionSearchCriteria;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionSearchResultItem;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionSourceNotFoundException;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @WebMvcTest} slice - HTTP layer only (status codes, JSON shape, validation, error
 * translation, and now authentication/authorization), the same use-case mock at every scenario.
 * No Testcontainers, no real database: this layer has nothing to do with persistence.
 *
 * <p>{@code SecurityConfig} is imported explicitly since it is a plain {@code @Configuration}
 * class, not one of {@code @WebMvcTest}'s default auto-included stereotypes. {@code JwtDecoder}
 * is mocked (rather than relying on the real issuer-based or local symmetric-key beans) purely to
 * satisfy {@code SecurityConfig}'s bean-wiring requirement without a network call or profile
 * dependency; most tests never invoke it directly, since {@code jwt()} constructs an
 * already-authenticated principal without going through the decoder at all - only the
 * malformed-token test stubs it.
 */
@WebMvcTest(TransactionController.class)
@Import(SecurityConfig.class)
class TransactionControllerTests {

	private static final String ACTOR = "jwt-subject-001";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private CreateTransactionUseCase createTransactionUseCase;

	@MockitoBean
	private CreateTransactionsBulkUseCase createTransactionsBulkUseCase;

	@MockitoBean
	private GetTransactionUseCase getTransactionUseCase;

	@MockitoBean
	private SearchTransactionsUseCase searchTransactionsUseCase;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	private static CreateTransactionRequest aRequest() {
		return new CreateTransactionRequest(UUID.randomUUID(), "MOCK_BANK_A", "EXT-001", "Checkers",
				new BigDecimal("125.50"), "ZAR", "DEBIT", "groceries", Instant.parse("2026-08-06T08:00:00Z"));
	}

	private static TransactionCreatedResult aResult(UUID customerId) {
		UUID merchantId = UUID.randomUUID();
		return new TransactionCreatedResult(UUID.randomUUID(), customerId, "MOCK_BANK_A", "EXT-001",
				merchantId, "Checkers", "GROCERIES", "Groceries", new BigDecimal("125.50"), "ZAR", "DEBIT",
				"groceries", "PROCESSED", Instant.parse("2026-08-06T08:00:00Z"),
				Instant.parse("2026-08-06T08:00:01Z"), Instant.parse("2026-08-06T08:00:01Z"));
	}

	private static BulkCreateTransactionsRequest aBulkRequest(int itemCount) {
		return new BulkCreateTransactionsRequest(java.util.stream.IntStream.range(0, itemCount)
				.mapToObj(i -> new CreateTransactionRequest(UUID.randomUUID(), "MOCK_BANK_A", "EXT-" + i,
						"Checkers", new BigDecimal("125.50"), "ZAR", "DEBIT", "groceries",
						Instant.parse("2026-08-06T08:00:00Z")))
				.toList());
	}

	private static RequestPostProcessor transactionWriteToken() {
		return jwt().jwt(builder -> builder.subject(ACTOR)).authorities(new SimpleGrantedAuthority("TRANSACTION_WRITE"));
	}

	private static RequestPostProcessor transactionReadToken() {
		return jwt().jwt(builder -> builder.subject(ACTOR)).authorities(new SimpleGrantedAuthority("TRANSACTION_READ"));
	}

	private static TransactionDetails aDetails(UUID id) {
		UUID merchantId = UUID.randomUUID();
		return new TransactionDetails(id, UUID.randomUUID(), "MOCK_BANK_A", "EXT-001",
				merchantId, "Checkers", "GROCERIES", "Groceries", new BigDecimal("125.50"), "ZAR", "DEBIT",
				"groceries", "PROCESSED", Instant.parse("2026-08-06T08:00:00Z"),
				Instant.parse("2026-08-06T08:00:01Z"), Instant.parse("2026-08-06T08:00:01Z"));
	}

	private static TransactionSearchResultItem aSearchResultItem() {
		return new TransactionSearchResultItem(UUID.randomUUID(), UUID.randomUUID(), "Checkers", "GROCERIES",
				new BigDecimal("125.50"), "ZAR", "DEBIT", Instant.parse("2026-08-06T08:00:00Z"));
	}

	@Test
	void createsATransactionAndReturns201WithLocationHeaderAndFullBody() throws Exception {
		CreateTransactionRequest request = aRequest();
		TransactionCreatedResult result = aResult(request.customerId());
		when(createTransactionUseCase.create(any())).thenReturn(result);

		mockMvc.perform(post("/api/v1/transactions")
						.with(transactionWriteToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.id").value(result.id().toString()))
				.andExpect(jsonPath("$.customerId").value(result.customerId().toString()))
				.andExpect(jsonPath("$.merchant.id").value(result.merchantId().toString()))
				.andExpect(jsonPath("$.merchant.displayName").value("Checkers"))
				.andExpect(jsonPath("$.category.code").value("GROCERIES"))
				.andExpect(jsonPath("$.category.name").value("Groceries"))
				.andExpect(jsonPath("$.status").value("PROCESSED"));
	}

	@Test
	void propagatesTheAuthenticatedJwtSubjectAsTheActor() throws Exception {
		CreateTransactionRequest request = aRequest();
		when(createTransactionUseCase.create(any())).thenReturn(aResult(request.customerId()));

		mockMvc.perform(post("/api/v1/transactions")
						.with(transactionWriteToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated());

		ArgumentCaptor<CreateTransactionCommand> captor = ArgumentCaptor.forClass(CreateTransactionCommand.class);
		verify(createTransactionUseCase).create(captor.capture());
		assertThat(captor.getValue().actor()).isEqualTo(ACTOR);
	}

	@Test
	void echoesBackAClientSuppliedCorrelationIdHeader() throws Exception {
		CreateTransactionRequest request = aRequest();
		when(createTransactionUseCase.create(any())).thenReturn(aResult(request.customerId()));

		mockMvc.perform(post("/api/v1/transactions")
						.with(transactionWriteToken())
						.header(CorrelationId.HEADER_NAME, "client-correlation-id")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(header().string(CorrelationId.HEADER_NAME, "client-correlation-id"));
	}

	@Test
	void generatesACorrelationIdWhenNoneIsSupplied() throws Exception {
		CreateTransactionRequest request = aRequest();
		when(createTransactionUseCase.create(any())).thenReturn(aResult(request.customerId()));

		mockMvc.perform(post("/api/v1/transactions")
						.with(transactionWriteToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(header().exists(CorrelationId.HEADER_NAME));
	}

	@Test
	void returnsBadRequestWhenCustomerIdIsMissing() throws Exception {
		String body = """
				{"sourceCode":"MOCK_BANK_A","externalTransactionId":"EXT-001","amount":10.00,\
				"currency":"ZAR","direction":"DEBIT","occurredAt":"2026-08-06T08:00:00Z"}""";

		mockMvc.perform(post("/api/v1/transactions").with(transactionWriteToken())
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("REQUEST_VALIDATION_FAILED"));
	}

	@Test
	void returnsBadRequestWhenAmountIsNotPositive() throws Exception {
		CreateTransactionRequest request = new CreateTransactionRequest(UUID.randomUUID(), "MOCK_BANK_A", "EXT-001",
				"Checkers", new BigDecimal("-5.00"), "ZAR", "DEBIT", "groceries", Instant.parse("2026-08-06T08:00:00Z"));

		mockMvc.perform(post("/api/v1/transactions")
						.with(transactionWriteToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returnsConflictWithTransactionDuplicateWhenTheUseCaseSignalsADuplicate() throws Exception {
		CreateTransactionRequest request = aRequest();
		when(createTransactionUseCase.create(any()))
				.thenThrow(new DuplicateTransactionException(UUID.randomUUID(), "EXT-001"));

		mockMvc.perform(post("/api/v1/transactions")
						.with(transactionWriteToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("TRANSACTION_DUPLICATE"));
	}

	@Test
	void returnsNotFoundWithSourceNotFoundWhenTheSourceDoesNotExist() throws Exception {
		CreateTransactionRequest request = aRequest();
		when(createTransactionUseCase.create(any())).thenThrow(new TransactionSourceNotFoundException("MOCK_BANK_A"));

		mockMvc.perform(post("/api/v1/transactions")
						.with(transactionWriteToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("SOURCE_NOT_FOUND"));
	}

	@Test
	void returnsNotFoundWithCustomerNotFoundWhenTheCustomerDoesNotExist() throws Exception {
		CreateTransactionRequest request = aRequest();
		when(createTransactionUseCase.create(any())).thenThrow(new CustomerNotFoundException(request.customerId()));

		mockMvc.perform(post("/api/v1/transactions")
						.with(transactionWriteToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("CUSTOMER_NOT_FOUND"));
	}

	@Test
	void returnsUnauthorizedWithAuthenticationRequiredWhenNoTokenIsSupplied() throws Exception {
		CreateTransactionRequest request = aRequest();

		mockMvc.perform(post("/api/v1/transactions")
						.header(CorrelationId.HEADER_NAME, "client-correlation-id")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"))
				.andExpect(header().string(CorrelationId.HEADER_NAME, "client-correlation-id"));
	}

	@Test
	void returnsUnauthorizedWithTokenInvalidWhenTheTokenIsMalformedOrExpired() throws Exception {
		when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("expired or malformed"));
		CreateTransactionRequest request = aRequest();

		mockMvc.perform(post("/api/v1/transactions")
						.header("Authorization", "Bearer not-a-real-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("TOKEN_INVALID"));
	}

	@Test
	void returnsForbiddenWithAccessDeniedWhenTheAuthorityIsMissing() throws Exception {
		CreateTransactionRequest request = aRequest();

		mockMvc.perform(post("/api/v1/transactions")
						.with(jwt().jwt(builder -> builder.subject(ACTOR)).authorities(new SimpleGrantedAuthority("AGGREGATION_READ")))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
	}

	@Test
	void getReturnsTheTransactionWhenFound() throws Exception {
		UUID id = UUID.randomUUID();
		when(getTransactionUseCase.get(id)).thenReturn(aDetails(id));

		mockMvc.perform(get("/api/v1/transactions/{id}", id).with(transactionReadToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.merchant.displayName").value("Checkers"))
				.andExpect(jsonPath("$.category.code").value("GROCERIES"))
				.andExpect(jsonPath("$.status").value("PROCESSED"));
	}

	@Test
	void getReturnsNotFoundWithTransactionNotFoundWhenNoTransactionMatches() throws Exception {
		UUID id = UUID.randomUUID();
		when(getTransactionUseCase.get(id)).thenThrow(new TransactionNotFoundException(id));

		mockMvc.perform(get("/api/v1/transactions/{id}", id).with(transactionReadToken()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("TRANSACTION_NOT_FOUND"));
	}

	@Test
	void getReturnsUnauthorizedWhenNoTokenIsSupplied() throws Exception {
		mockMvc.perform(get("/api/v1/transactions/{id}", UUID.randomUUID()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	void getReturnsForbiddenWhenTheAuthorityIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/transactions/{id}", UUID.randomUUID())
						.with(transactionWriteToken()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
	}

	@Test
	void searchReturnsAPageOfResultsWithDefaultPagination() throws Exception {
		PagedResult<TransactionSearchResultItem> result = new PagedResult<>(List.of(aSearchResultItem()), 0, 20, 1, 1);
		when(searchTransactionsUseCase.search(any())).thenReturn(result);

		mockMvc.perform(get("/api/v1/transactions").with(transactionReadToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].merchantName").value("Checkers"))
				.andExpect(jsonPath("$.content[0].categoryCode").value("GROCERIES"))
				.andExpect(jsonPath("$.page.number").value(0))
				.andExpect(jsonPath("$.page.size").value(20))
				.andExpect(jsonPath("$.page.totalElements").value(1))
				.andExpect(jsonPath("$.page.totalPages").value(1));
	}

	@Test
	void searchPassesQueryParametersThroughToTheCriteria() throws Exception {
		UUID customerId = UUID.randomUUID();
		UUID merchantId = UUID.randomUUID();
		when(searchTransactionsUseCase.search(any())).thenReturn(new PagedResult<>(List.of(), 1, 10, 0, 0));

		mockMvc.perform(get("/api/v1/transactions")
						.with(transactionReadToken())
						.param("customerId", customerId.toString())
						.param("sourceCode", "MOCK_BANK_A")
						.param("categoryCode", "GROCERIES")
						.param("merchantId", merchantId.toString())
						.param("direction", "DEBIT")
						.param("status", "PROCESSED")
						.param("occurredFrom", "2026-01-01T00:00:00Z")
						.param("occurredTo", "2026-01-31T23:59:59Z")
						.param("page", "1")
						.param("size", "10")
						.param("sort", "transactionTimestamp,asc"))
				.andExpect(status().isOk());

		ArgumentCaptor<TransactionSearchCriteria> captor = ArgumentCaptor.forClass(TransactionSearchCriteria.class);
		verify(searchTransactionsUseCase).search(captor.capture());
		TransactionSearchCriteria criteria = captor.getValue();
		assertThat(criteria.customerId()).isEqualTo(customerId);
		assertThat(criteria.sourceCode()).isEqualTo("MOCK_BANK_A");
		assertThat(criteria.categoryCode()).isEqualTo("GROCERIES");
		assertThat(criteria.merchantId()).isEqualTo(merchantId);
		assertThat(criteria.direction()).isEqualTo("DEBIT");
		assertThat(criteria.status()).isEqualTo("PROCESSED");
		assertThat(criteria.page()).isEqualTo(1);
		assertThat(criteria.size()).isEqualTo(10);
		assertThat(criteria.sort()).isEqualTo("transactionTimestamp,asc");
	}

	@Test
	void searchReturnsBadRequestWhenTheUseCaseSignalsAValidationFailure() throws Exception {
		when(searchTransactionsUseCase.search(any()))
				.thenThrow(new TransactionValidationException("size must be between 1 and 100", null));

		mockMvc.perform(get("/api/v1/transactions").with(transactionReadToken()).param("size", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("REQUEST_VALIDATION_FAILED"));
	}

	@Test
	void searchReturnsUnauthorizedWhenNoTokenIsSupplied() throws Exception {
		mockMvc.perform(get("/api/v1/transactions"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	void searchReturnsForbiddenWhenTheAuthorityIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/transactions").with(transactionWriteToken()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
	}

	@Test
	void bulkCreateReturns207WithTheMappedResultOnAMixedOutcomeBatch() throws Exception {
		BulkTransactionResult result = new BulkTransactionResult(2, 1, 1, List.of(
				BulkTransactionItemResult.created(UUID.randomUUID()),
				BulkTransactionItemResult.failed("TRANSACTION_DUPLICATE", "already exists")));
		when(createTransactionsBulkUseCase.create(any())).thenReturn(result);

		mockMvc.perform(post("/api/v1/transactions/bulk")
						.with(transactionWriteToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(aBulkRequest(2))))
				.andExpect(status().isMultiStatus())
				.andExpect(jsonPath("$.total").value(2))
				.andExpect(jsonPath("$.successful").value(1))
				.andExpect(jsonPath("$.failed").value(1))
				.andExpect(jsonPath("$.results[0].index").value(0))
				.andExpect(jsonPath("$.results[0].status").value("CREATED"))
				.andExpect(jsonPath("$.results[1].index").value(1))
				.andExpect(jsonPath("$.results[1].status").value("CONFLICT"))
				.andExpect(jsonPath("$.results[1].errorCode").value("TRANSACTION_DUPLICATE"));
	}

	@Test
	void bulkCreateReturns207ForAFullyFailedBatch() throws Exception {
		BulkTransactionResult result = new BulkTransactionResult(1, 0, 1, List.of(
				BulkTransactionItemResult.failed("REQUEST_VALIDATION_FAILED", "amount must be greater than zero")));
		when(createTransactionsBulkUseCase.create(any())).thenReturn(result);

		mockMvc.perform(post("/api/v1/transactions/bulk")
						.with(transactionWriteToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(aBulkRequest(1))))
				.andExpect(status().isMultiStatus())
				.andExpect(jsonPath("$.results[0].status").value("FAILED"))
				.andExpect(jsonPath("$.results[0].errorCode").value("REQUEST_VALIDATION_FAILED"));
	}

	@Test
	void bulkCreateReturnsBadRequestWhenTheBatchIsEmpty() throws Exception {
		mockMvc.perform(post("/api/v1/transactions/bulk")
						.with(transactionWriteToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(aBulkRequest(0))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("REQUEST_VALIDATION_FAILED"));
	}

	@Test
	void bulkCreateReturnsBadRequestWhenTheBatchExceedsTheMaximum() throws Exception {
		mockMvc.perform(post("/api/v1/transactions/bulk")
						.with(transactionWriteToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(aBulkRequest(501))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("REQUEST_VALIDATION_FAILED"));
	}

	@Test
	void bulkCreateReturnsUnauthorizedWhenNoTokenIsSupplied() throws Exception {
		mockMvc.perform(post("/api/v1/transactions/bulk")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(aBulkRequest(1))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	void bulkCreateReturnsForbiddenWhenTheAuthorityIsMissing() throws Exception {
		mockMvc.perform(post("/api/v1/transactions/bulk")
						.with(jwt().jwt(builder -> builder.subject(ACTOR)).authorities(new SimpleGrantedAuthority("TRANSACTION_READ")))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(aBulkRequest(1))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
	}

}
