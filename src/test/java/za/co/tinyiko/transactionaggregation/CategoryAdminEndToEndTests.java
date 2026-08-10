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

import za.co.tinyiko.transactionaggregation.api.dto.request.CreateCategorisationRuleRequest;
import za.co.tinyiko.transactionaggregation.api.dto.request.CreateTransactionRequest;
import za.co.tinyiko.transactionaggregation.api.dto.request.UpdateCategorisationRuleRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * One real, full-stack proof that a category-admin write is genuinely visible to runtime
 * transaction categorisation - not just that the admin endpoints work in isolation, and not just
 * that {@code CategorisationRuleEngine}/{@code CategorisationService} still pass their own
 * unchanged unit tests (they do; this is the integration risk those tests cannot cover): real
 * Postgres, real Flyway-seeded data left untouched, a brand-new rule created through the real
 * {@code CATEGORY_ADMIN}-secured admin API, then a real transaction created through the real
 * {@code TRANSACTION_WRITE}-secured transaction API, asserting the new rule actually drove the
 * categorisation outcome. A second scenario proves the reverse: deactivating a rule through the
 * same admin API stops it from being used by the very next transaction.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CategoryAdminEndToEndTests {

	private static final String ADMIN_ACTOR = "e2e-admin-subject";
	private static final String WRITE_ACTOR = "e2e-write-subject";

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

	private UUID categoryIdFor(String code) {
		return jdbcTemplate.queryForObject("SELECT id FROM transaction_categories WHERE code = ?", UUID.class, code);
	}

	@Test
	void aNewlyCreatedRuleIsUsedByTheVeryNextTransactionCreation() throws Exception {
		UUID customerId = insertCustomer();
		UUID entertainmentCategoryId = categoryIdFor("ENTERTAINMENT");
		String distinctiveKeyword = "ZZZTESTMERCHANT" + UUID.randomUUID().toString().substring(0, 8);

		CreateCategorisationRuleRequest ruleRequest = new CreateCategorisationRuleRequest(
				entertainmentCategoryId, "MERCHANT", "CONTAINS", distinctiveKeyword, "DEBIT", 1);
		mockMvc.perform(post("/api/v1/categorisation-rules")
						.with(jwt().jwt(builder -> builder.subject(ADMIN_ACTOR)).authorities(new SimpleGrantedAuthority("CATEGORY_ADMIN")))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(ruleRequest)))
				.andExpect(status().isCreated());

		CreateTransactionRequest transactionRequest = new CreateTransactionRequest(customerId, "MOCK_BANK_A",
				"EXT-E2E-" + UUID.randomUUID(), distinctiveKeyword + " Store", new BigDecimal("75.00"), "ZAR",
				"DEBIT", "streaming subscription", Instant.now());

		mockMvc.perform(post("/api/v1/transactions")
						.with(jwt().jwt(builder -> builder.subject(WRITE_ACTOR)).authorities(new SimpleGrantedAuthority("TRANSACTION_WRITE")))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(transactionRequest)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.category.code").value("ENTERTAINMENT"));
	}

	@Test
	void deactivatingARuleStopsItFromBeingUsedByTheVeryNextTransactionCreation() throws Exception {
		UUID customerId = insertCustomer();
		UUID fuelCategoryId = categoryIdFor("FUEL");
		String distinctiveKeyword = "ZZZDEACTIVATED" + UUID.randomUUID().toString().substring(0, 8);

		CreateCategorisationRuleRequest ruleRequest = new CreateCategorisationRuleRequest(
				fuelCategoryId, "MERCHANT", "CONTAINS", distinctiveKeyword, "DEBIT", 1);
		String createResponse = mockMvc.perform(post("/api/v1/categorisation-rules")
						.with(jwt().jwt(builder -> builder.subject(ADMIN_ACTOR)).authorities(new SimpleGrantedAuthority("CATEGORY_ADMIN")))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(ruleRequest)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		UUID ruleId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asText());

		UpdateCategorisationRuleRequest deactivateRequest = new UpdateCategorisationRuleRequest(
				fuelCategoryId, "MERCHANT", "CONTAINS", distinctiveKeyword, "DEBIT", 1, false, 0L);
		mockMvc.perform(put("/api/v1/categorisation-rules/{id}", ruleId)
						.with(jwt().jwt(builder -> builder.subject(ADMIN_ACTOR)).authorities(new SimpleGrantedAuthority("CATEGORY_ADMIN")))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(deactivateRequest)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));

		CreateTransactionRequest transactionRequest = new CreateTransactionRequest(customerId, "MOCK_BANK_A",
				"EXT-E2E-" + UUID.randomUUID(), distinctiveKeyword + " Store", new BigDecimal("75.00"), "ZAR",
				"DEBIT", "some purchase", Instant.now());

		String result = mockMvc.perform(post("/api/v1/transactions")
						.with(jwt().jwt(builder -> builder.subject(WRITE_ACTOR)).authorities(new SimpleGrantedAuthority("TRANSACTION_WRITE")))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(transactionRequest)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		assertThat(objectMapper.readTree(result).get("category").get("code").asText()).isNotEqualTo("FUEL");
	}

}
