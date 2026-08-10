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

import io.micrometer.core.instrument.MeterRegistry;

import tools.jackson.databind.ObjectMapper;

import za.co.tinyiko.transactionaggregation.api.dto.request.CreateTransactionRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real, full-stack observability coverage (feature/observability): real Testcontainers
 * PostgreSQL, real {@code security.SecurityConfig} filter chain, real {@code MeterRegistry} bean,
 * real {@code /actuator/prometheus}/{@code /actuator/metrics} endpoints. Counter assertions use a
 * before/after delta, not an absolute value - the Spring context (and its {@code MeterRegistry})
 * is shared across every {@code @Test} method in this class, so an absolute count would be
 * order-dependent and fragile.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ObservabilityEndToEndTests {

	private static final String ACTOR = "e2e-observability-actor";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MeterRegistry meterRegistry;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	private static org.springframework.test.web.servlet.request.RequestPostProcessor operationsReadToken() {
		return jwt().jwt(builder -> builder.subject(ACTOR)).authorities(new SimpleGrantedAuthority("OPERATIONS_READ"));
	}

	private UUID insertCustomer() {
		UUID id = UUID.randomUUID();
		jdbcTemplate.update(
				"INSERT INTO customers (id, external_reference, first_name, last_name, status, created_at, updated_at, version) "
						+ "VALUES (?, ?, 'Jane', 'Doe', 'ACTIVE', now(), now(), 0)",
				id, "EXT-CUST-" + id);
		return id;
	}

	private double counterValue(String name) {
		var counter = meterRegistry.find(name).counter();
		return counter == null ? 0.0 : counter.count();
	}

	@Test
	void healthRemainsPubliclyAccessibleAndUnchanged() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void prometheusAndMetricsRequireOperationsReadAuthority() throws Exception {
		mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());

		mockMvc.perform(get("/actuator/prometheus")
						.with(jwt().jwt(builder -> builder.subject(ACTOR)).authorities(new SimpleGrantedAuthority("TRANSACTION_READ"))))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/actuator/prometheus").with(operationsReadToken()))
				.andExpect(status().isOk());
		mockMvc.perform(get("/actuator/metrics").with(operationsReadToken()))
				.andExpect(status().isOk());
	}

	@Test
	void metricsDrillDownSubPathIsCoveredByTheSameAuthorityRule() throws Exception {
		mockMvc.perform(get("/actuator/metrics/jvm.memory.used")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/actuator/metrics/jvm.memory.used").with(operationsReadToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("jvm.memory.used"));
	}

	@Test
	void prometheusEndpointExportsBuiltInAndCustomMetricNamesInTheExpectedPrometheusFormat() throws Exception {
		UUID customerId = insertCustomer();
		CreateTransactionRequest request = new CreateTransactionRequest(customerId, "MOCK_BANK_A",
				"EXT-OBS-" + UUID.randomUUID(), "Checkers", new BigDecimal("75.00"), "ZAR", "DEBIT",
				"groceries", Instant.now());
		mockMvc.perform(post("/api/v1/transactions")
						.with(jwt().jwt(builder -> builder.subject(ACTOR)).authorities(new SimpleGrantedAuthority("TRANSACTION_WRITE")))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/actuator/prometheus").with(operationsReadToken()))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/plain"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("transactions_received_total")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("transactions_processed_total")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("transactions_rejected_total")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("transactions_duplicates_total")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("transactions_categorisation_fallback_total")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("transactions_processing_duration_seconds")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("http_server_requests_seconds")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("jvm_memory_used_bytes")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("hikaricp_connections")));
	}

	@Test
	void realTransactionCreationIncrementsReceivedAndProcessedCounters() throws Exception {
		double receivedBefore = counterValue("transactions.received");
		double processedBefore = counterValue("transactions.processed");

		UUID customerId = insertCustomer();
		CreateTransactionRequest request = new CreateTransactionRequest(customerId, "MOCK_BANK_A",
				"EXT-OBS-" + UUID.randomUUID(), "Checkers", new BigDecimal("75.00"), "ZAR", "DEBIT",
				"groceries", Instant.now());

		mockMvc.perform(post("/api/v1/transactions")
						.with(jwt().jwt(builder -> builder.subject(ACTOR)).authorities(new SimpleGrantedAuthority("TRANSACTION_WRITE")))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated());

		assertThat(counterValue("transactions.received")).isEqualTo(receivedBefore + 1.0);
		assertThat(counterValue("transactions.processed")).isEqualTo(processedBefore + 1.0);
	}

	@Test
	void realDuplicateTransactionIncrementsTheDuplicatesCounterNotTheGenericRejectedCounter() throws Exception {
		UUID customerId = insertCustomer();
		String externalId = "EXT-OBS-DUP-" + UUID.randomUUID();
		CreateTransactionRequest request = new CreateTransactionRequest(customerId, "MOCK_BANK_A",
				externalId, "Checkers", new BigDecimal("75.00"), "ZAR", "DEBIT", "groceries", Instant.now());

		mockMvc.perform(post("/api/v1/transactions")
						.with(jwt().jwt(builder -> builder.subject(ACTOR)).authorities(new SimpleGrantedAuthority("TRANSACTION_WRITE")))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated());

		double duplicatesBefore = counterValue("transactions.duplicates");

		mockMvc.perform(post("/api/v1/transactions")
						.with(jwt().jwt(builder -> builder.subject(ACTOR)).authorities(new SimpleGrantedAuthority("TRANSACTION_WRITE")))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isConflict());

		assertThat(counterValue("transactions.duplicates")).isEqualTo(duplicatesBefore + 1.0);
	}

	@Test
	void httpServerRequestsMeterExistsAfterARealRequest() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());

		assertThat(meterRegistry.find("http.server.requests").timers()).isNotEmpty();
	}

}
