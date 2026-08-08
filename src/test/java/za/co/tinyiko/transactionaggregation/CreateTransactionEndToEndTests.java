package za.co.tinyiko.transactionaggregation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import za.co.tinyiko.transactionaggregation.api.dto.request.CreateTransactionRequest;
import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * One real, full-stack happy-path smoke test for {@code POST /api/v1/transactions}: real
 * Testcontainers PostgreSQL, real Flyway migrations, real {@code CorrelationIdFilter}, real
 * permit-all {@code SecurityConfig} chain, real controller, real {@code CreateTransactionUseCase}
 * orchestration (customer/source/merchant/categorisation/audit), real persistence - proving the
 * whole {@code feature/api} wiring actually works end-to-end, once. This deliberately does not
 * re-verify every scenario already covered by {@code TransactionControllerTests}'s
 * {@code @WebMvcTest} slice (validation, each error mapping) - those don't need a database, this
 * one exists purely to catch a wiring mistake that a mocked-use-case slice test cannot.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CreateTransactionEndToEndTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private UUID insertCustomer() {
		UUID id = UUID.randomUUID();
		jdbcTemplate.update(
				"INSERT INTO customers (id, external_reference, first_name, last_name, status, created_at, updated_at, version) "
						+ "VALUES (?, ?, 'Jane', 'Doe', 'ACTIVE', now(), now(), 0)",
				id, "EXT-CUST-" + id);
		return id;
	}

	@Test
	void createsATransactionThroughTheWholeRealStack() throws Exception {
		UUID customerId = insertCustomer();
		CreateTransactionRequest request = new CreateTransactionRequest(customerId, "MOCK_BANK_A",
				"EXT-E2E-" + UUID.randomUUID(), "Checkers", new BigDecimal("125.50"), "ZAR", "DEBIT",
				"groceries", Instant.now());

		mockMvc.perform(post("/api/v1/transactions")
						.header(CorrelationId.HEADER_NAME, "e2e-correlation-id")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(header().string(CorrelationId.HEADER_NAME, "e2e-correlation-id"))
				.andExpect(jsonPath("$.customerId").value(customerId.toString()))
				.andExpect(jsonPath("$.status").value("PROCESSED"))
				.andExpect(jsonPath("$.merchant.displayName").value("Checkers"))
				.andExpect(jsonPath("$.category.code").exists());

		Long auditCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM audit_events WHERE correlation_id = ? AND event_type = 'TRANSACTION_CREATED'",
				Long.class, "e2e-correlation-id");
		assertThat(auditCount).isEqualTo(1L);
	}

}
