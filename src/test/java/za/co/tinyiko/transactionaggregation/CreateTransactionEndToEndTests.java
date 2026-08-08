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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * One real, full-stack happy-path smoke test for {@code POST /api/v1/transactions}: real
 * Testcontainers PostgreSQL, real Flyway migrations, real {@code CorrelationIdFilter}, real
 * {@code security.SecurityConfig} filter chain (JWT authentication + {@code @PreAuthorize}), real
 * controller, real {@code CreateTransactionUseCase} orchestration (customer/source/merchant/
 * categorisation/audit), real persistence - proving the whole wiring actually works end-to-end,
 * once, including that the authenticated actor reaches the persisted audit row.
 *
 * <p>{@code JwtDecoder} is mocked purely to satisfy {@code SecurityConfig}'s bean-wiring
 * requirement without a real issuer/network call - authentication itself is driven by the
 * {@code jwt()} request post-processor, which builds an already-authenticated principal directly,
 * the same technique used by the {@code @WebMvcTest} slices.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CreateTransactionEndToEndTests {

	private static final String ACTOR = "e2e-jwt-subject";

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
	void createsATransactionThroughTheWholeRealStackWithAuthenticatedActor() throws Exception {
		UUID customerId = insertCustomer();
		CreateTransactionRequest request = new CreateTransactionRequest(customerId, "MOCK_BANK_A",
				"EXT-E2E-" + UUID.randomUUID(), "Checkers", new BigDecimal("125.50"), "ZAR", "DEBIT",
				"groceries", Instant.now());

		mockMvc.perform(post("/api/v1/transactions")
						.with(jwt().jwt(builder -> builder.subject(ACTOR)).authorities(new SimpleGrantedAuthority("TRANSACTION_WRITE")))
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
				"SELECT COUNT(*) FROM audit_events WHERE correlation_id = ? AND event_type = 'TRANSACTION_CREATED' AND actor = ?",
				Long.class, "e2e-correlation-id", ACTOR);
		assertThat(auditCount).isEqualTo(1L);
	}

	@Test
	void rejectsAnUnauthenticatedRequestBeforeItReachesTheUseCase() throws Exception {
		UUID customerId = insertCustomer();
		CreateTransactionRequest request = new CreateTransactionRequest(customerId, "MOCK_BANK_A",
				"EXT-E2E-" + UUID.randomUUID(), "Checkers", new BigDecimal("125.50"), "ZAR", "DEBIT",
				"groceries", Instant.now());

		mockMvc.perform(post("/api/v1/transactions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"));
	}

}
