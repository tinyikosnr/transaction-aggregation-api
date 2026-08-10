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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import za.co.tinyiko.transactionaggregation.api.dto.request.CreateTransactionRequest;
import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves audit-query is genuinely visible against real audit rows written by the real
 * transaction-creation workflow - not just that {@code GET /api/v1/audit-events} works against
 * mocked data. Uses only the existing, real {@code POST /api/v1/transactions} endpoint to produce
 * audit rows; no artificial direct-insert write path.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuditQueryEndToEndTests {

	private static final String WRITE_ACTOR = "e2e-audit-write-subject";
	private static final String READ_ACTOR = "e2e-audit-read-subject";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	private UUID insertCustomer() {
		UUID id = UUID.randomUUID();
		jdbcTemplate.update(
				"INSERT INTO customers (id, external_reference, first_name, last_name, status, created_at, updated_at, version) "
						+ "VALUES (?, ?, 'Jane', 'Doe', 'ACTIVE', now(), now(), 0)",
				id, "EXT-CUST-" + id);
		return id;
	}

	@Test
	void aRealCreatedTransactionsAuditEventIsDiscoverableByCorrelationIdAndAggregateId() throws Exception {
		UUID customerId = insertCustomer();
		String correlationId = "e2e-audit-corr-" + UUID.randomUUID();
		CreateTransactionRequest request = new CreateTransactionRequest(customerId, "MOCK_BANK_A",
				"EXT-E2E-" + UUID.randomUUID(), "Checkers", new BigDecimal("125.50"), "ZAR", "DEBIT",
				"groceries", Instant.now());

		String createResponse = mockMvc.perform(post("/api/v1/transactions")
						.with(jwt().jwt(builder -> builder.subject(WRITE_ACTOR)).authorities(new SimpleGrantedAuthority("TRANSACTION_WRITE")))
						.header(CorrelationId.HEADER_NAME, correlationId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		UUID transactionId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asString());

		mockMvc.perform(get("/api/v1/audit-events")
						.with(jwt().jwt(builder -> builder.subject(READ_ACTOR)).authorities(new SimpleGrantedAuthority("AUDIT_READ")))
						.param("correlationId", correlationId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
				.andExpect(jsonPath("$.content[0].eventType").value("TRANSACTION_CREATED"))
				.andExpect(jsonPath("$.content[0].aggregateType").value("TRANSACTION"))
				.andExpect(jsonPath("$.content[0].aggregateId").value(transactionId.toString()))
				.andExpect(jsonPath("$.content[0].actor").value(WRITE_ACTOR))
				.andExpect(jsonPath("$.content[0].eventData.categoryId").exists());

		mockMvc.perform(get("/api/v1/audit-events")
						.with(jwt().jwt(builder -> builder.subject(READ_ACTOR)).authorities(new SimpleGrantedAuthority("AUDIT_READ")))
						.param("aggregateType", "TRANSACTION")
						.param("aggregateId", transactionId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
				.andExpect(jsonPath("$.content[0].eventType").value("TRANSACTION_CREATED"));
	}

	@Test
	void aDuplicateTransactionRejectionIsDiscoverableByCorrelationId() throws Exception {
		UUID customerId = insertCustomer();
		String externalId = "EXT-E2E-DUP-" + UUID.randomUUID();
		CreateTransactionRequest firstRequest = new CreateTransactionRequest(customerId, "MOCK_BANK_A",
				externalId, "Checkers", new BigDecimal("125.50"), "ZAR", "DEBIT", "groceries", Instant.now());

		mockMvc.perform(post("/api/v1/transactions")
						.with(jwt().jwt(builder -> builder.subject(WRITE_ACTOR)).authorities(new SimpleGrantedAuthority("TRANSACTION_WRITE")))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(firstRequest)))
				.andExpect(status().isCreated());

		String duplicateCorrelationId = "e2e-audit-dup-corr-" + UUID.randomUUID();
		CreateTransactionRequest duplicateRequest = new CreateTransactionRequest(customerId, "MOCK_BANK_A",
				externalId, "Checkers", new BigDecimal("125.50"), "ZAR", "DEBIT", "groceries", Instant.now());

		mockMvc.perform(post("/api/v1/transactions")
						.with(jwt().jwt(builder -> builder.subject(WRITE_ACTOR)).authorities(new SimpleGrantedAuthority("TRANSACTION_WRITE")))
						.header(CorrelationId.HEADER_NAME, duplicateCorrelationId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(duplicateRequest)))
				.andExpect(status().isConflict());

		mockMvc.perform(get("/api/v1/audit-events")
						.with(jwt().jwt(builder -> builder.subject(READ_ACTOR)).authorities(new SimpleGrantedAuthority("AUDIT_READ")))
						.param("correlationId", duplicateCorrelationId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
				.andExpect(jsonPath("$.content[0].eventType").value("TRANSACTION_DUPLICATE_REJECTED"))
				.andExpect(jsonPath("$.content[0].actor").value(WRITE_ACTOR));
	}

	@Test
	void auditQueryWithoutAuditReadAuthorityIsForbidden() throws Exception {
		mockMvc.perform(get("/api/v1/audit-events")
						.with(jwt().jwt(builder -> builder.subject(READ_ACTOR)).authorities(new SimpleGrantedAuthority("TRANSACTION_READ"))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
	}

	@Test
	void auditQueryRejectsAggregateIdWithoutAggregateType() throws Exception {
		mockMvc.perform(get("/api/v1/audit-events")
						.with(jwt().jwt(builder -> builder.subject(READ_ACTOR)).authorities(new SimpleGrantedAuthority("AUDIT_READ")))
						.param("aggregateId", UUID.randomUUID().toString()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("REQUEST_VALIDATION_FAILED"));
	}

}
