package za.co.tinyiko.transactionaggregation.api.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import za.co.tinyiko.transactionaggregation.api.dto.request.CreateTransactionRequest;
import za.co.tinyiko.transactionaggregation.config.SecurityConfig;
import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;
import za.co.tinyiko.transactionaggregation.transaction.application.CreateTransactionUseCase;
import za.co.tinyiko.transactionaggregation.transaction.application.CustomerNotFoundException;
import za.co.tinyiko.transactionaggregation.transaction.application.DuplicateTransactionException;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionCreatedResult;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionSourceNotFoundException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @WebMvcTest} slice - HTTP layer only (status codes, JSON shape, validation, error
 * translation), the same use-case mock at every scenario. No Testcontainers, no real database:
 * this layer has nothing to do with persistence. {@code SecurityConfig} is imported explicitly
 * since it is a plain {@code @Configuration} class, not one of {@code @WebMvcTest}'s default
 * auto-included stereotypes (unlike {@code CorrelationIdFilter}/{@code GlobalExceptionHandler},
 * which are picked up automatically as a {@code Filter} and {@code @RestControllerAdvice}).
 */
@WebMvcTest(TransactionController.class)
@Import(SecurityConfig.class)
class TransactionControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private CreateTransactionUseCase createTransactionUseCase;

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

	@Test
	void createsATransactionAndReturns201WithLocationHeaderAndFullBody() throws Exception {
		CreateTransactionRequest request = aRequest();
		TransactionCreatedResult result = aResult(request.customerId());
		when(createTransactionUseCase.create(any())).thenReturn(result);

		mockMvc.perform(post("/api/v1/transactions")
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
	void echoesBackAClientSuppliedCorrelationIdHeader() throws Exception {
		CreateTransactionRequest request = aRequest();
		when(createTransactionUseCase.create(any())).thenReturn(aResult(request.customerId()));

		mockMvc.perform(post("/api/v1/transactions")
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
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(header().exists(CorrelationId.HEADER_NAME));
	}

	@Test
	void returnsBadRequestWhenCustomerIdIsMissing() throws Exception {
		String body = """
				{"sourceCode":"MOCK_BANK_A","externalTransactionId":"EXT-001","amount":10.00,\
				"currency":"ZAR","direction":"DEBIT","occurredAt":"2026-08-06T08:00:00Z"}""";

		mockMvc.perform(post("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
	}

	@Test
	void returnsBadRequestWhenAmountIsNotPositive() throws Exception {
		CreateTransactionRequest request = new CreateTransactionRequest(UUID.randomUUID(), "MOCK_BANK_A", "EXT-001",
				"Checkers", new BigDecimal("-5.00"), "ZAR", "DEBIT", "groceries", Instant.parse("2026-08-06T08:00:00Z"));

		mockMvc.perform(post("/api/v1/transactions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returnsConflictWithTrx001WhenTheUseCaseSignalsADuplicate() throws Exception {
		CreateTransactionRequest request = aRequest();
		when(createTransactionUseCase.create(any()))
				.thenThrow(new DuplicateTransactionException(UUID.randomUUID(), "EXT-001"));

		mockMvc.perform(post("/api/v1/transactions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("TRX-001"));
	}

	@Test
	void returnsNotFoundWithSrc001WhenTheSourceDoesNotExist() throws Exception {
		CreateTransactionRequest request = aRequest();
		when(createTransactionUseCase.create(any())).thenThrow(new TransactionSourceNotFoundException("MOCK_BANK_A"));

		mockMvc.perform(post("/api/v1/transactions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("SRC-001"));
	}

	@Test
	void returnsNotFoundWithCus001WhenTheCustomerDoesNotExist() throws Exception {
		CreateTransactionRequest request = aRequest();
		when(createTransactionUseCase.create(any())).thenThrow(new CustomerNotFoundException(request.customerId()));

		mockMvc.perform(post("/api/v1/transactions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("CUS-001"));
	}

}
