package za.co.tinyiko.transactionaggregation.api.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import tools.jackson.databind.ObjectMapper;

import za.co.tinyiko.transactionaggregation.api.dto.request.CreateCategorisationRuleRequest;
import za.co.tinyiko.transactionaggregation.api.dto.request.UpdateCategorisationRuleRequest;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategoryAdminView;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategoryNotFoundException;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategorisationRuleView;
import za.co.tinyiko.transactionaggregation.categorisation.application.CreateCategorisationRuleUseCase;
import za.co.tinyiko.transactionaggregation.categorisation.application.GetCategorisationRuleUseCase;
import za.co.tinyiko.transactionaggregation.categorisation.application.ListCategoriesUseCase;
import za.co.tinyiko.transactionaggregation.categorisation.application.ListCategorisationRulesUseCase;
import za.co.tinyiko.transactionaggregation.categorisation.application.RuleConflictException;
import za.co.tinyiko.transactionaggregation.categorisation.application.RuleNotFoundException;
import za.co.tinyiko.transactionaggregation.categorisation.application.UpdateCategorisationRuleUseCase;
import za.co.tinyiko.transactionaggregation.security.SecurityConfig;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryAdminController.class)
@Import(SecurityConfig.class)
class CategoryAdminControllerTests {

	private static final String ACTOR = "jwt-subject-001";
	private static final UUID CATEGORY_ID = UUID.randomUUID();
	private static final UUID RULE_ID = UUID.randomUUID();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private ListCategoriesUseCase listCategoriesUseCase;

	@MockitoBean
	private ListCategorisationRulesUseCase listCategorisationRulesUseCase;

	@MockitoBean
	private GetCategorisationRuleUseCase getCategorisationRuleUseCase;

	@MockitoBean
	private CreateCategorisationRuleUseCase createCategorisationRuleUseCase;

	@MockitoBean
	private UpdateCategorisationRuleUseCase updateCategorisationRuleUseCase;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	private static RequestPostProcessor adminToken() {
		return jwt().jwt(builder -> builder.subject(ACTOR)).authorities(new SimpleGrantedAuthority("CATEGORY_ADMIN"));
	}

	private static CategoryAdminView aCategoryView() {
		return new CategoryAdminView(CATEGORY_ID, "GROCERIES", "Groceries", "desc", false, true,
				Instant.parse("2026-08-10T08:00:00Z"), Instant.parse("2026-08-10T08:00:00Z"));
	}

	private static CategorisationRuleView aRuleView(long version) {
		return new CategorisationRuleView(RULE_ID, CATEGORY_ID, "MERCHANT", "CONTAINS", "SHELL", "DEBIT", 30, true,
				version, Instant.parse("2026-08-10T08:00:00Z"), Instant.parse("2026-08-10T08:00:00Z"));
	}

	private static CreateCategorisationRuleRequest aCreateRequest() {
		return new CreateCategorisationRuleRequest(CATEGORY_ID, "MERCHANT", "CONTAINS", "SHELL", "DEBIT", 30);
	}

	private static UpdateCategorisationRuleRequest anUpdateRequest() {
		return new UpdateCategorisationRuleRequest(CATEGORY_ID, "MERCHANT", "CONTAINS", "SHELL", "DEBIT", 30, true, 0L);
	}

	// --- categories ---

	@Test
	void listCategoriesReturnsOkWithTheMappedList() throws Exception {
		when(listCategoriesUseCase.list()).thenReturn(List.of(aCategoryView()));

		mockMvc.perform(get("/api/v1/categories").with(adminToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].code").value("GROCERIES"))
				.andExpect(jsonPath("$[0].fallback").value(false));
	}

	@Test
	void getCategoryReturnsOkWhenFound() throws Exception {
		when(listCategoriesUseCase.get(CATEGORY_ID)).thenReturn(aCategoryView());

		mockMvc.perform(get("/api/v1/categories/{id}", CATEGORY_ID).with(adminToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(CATEGORY_ID.toString()));
	}

	@Test
	void getCategoryReturnsNotFoundWithCategoryNotFound() throws Exception {
		when(listCategoriesUseCase.get(CATEGORY_ID)).thenThrow(new CategoryNotFoundException(CATEGORY_ID));

		mockMvc.perform(get("/api/v1/categories/{id}", CATEGORY_ID).with(adminToken()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("CATEGORY_NOT_FOUND"));
	}

	@Test
	void listCategoriesReturnsUnauthorizedWhenNoTokenIsSupplied() throws Exception {
		mockMvc.perform(get("/api/v1/categories"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	void listCategoriesReturnsForbiddenWhenTheAuthorityIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/categories")
						.with(jwt().jwt(builder -> builder.subject(ACTOR)).authorities(new SimpleGrantedAuthority("TRANSACTION_READ"))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
	}

	// --- rules: reads ---

	@Test
	void listRulesReturnsOkWithVersionInEachRow() throws Exception {
		when(listCategorisationRulesUseCase.list()).thenReturn(List.of(aRuleView(3L)));

		mockMvc.perform(get("/api/v1/categorisation-rules").with(adminToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].version").value(3));
	}

	@Test
	void getRuleReturnsOkWhenFound() throws Exception {
		when(getCategorisationRuleUseCase.get(RULE_ID)).thenReturn(aRuleView(0L));

		mockMvc.perform(get("/api/v1/categorisation-rules/{id}", RULE_ID).with(adminToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(RULE_ID.toString()));
	}

	@Test
	void getRuleReturnsNotFoundWithRuleNotFound() throws Exception {
		when(getCategorisationRuleUseCase.get(RULE_ID)).thenThrow(new RuleNotFoundException(RULE_ID));

		mockMvc.perform(get("/api/v1/categorisation-rules/{id}", RULE_ID).with(adminToken()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("RULE_NOT_FOUND"));
	}

	// --- rules: create ---

	@Test
	void createRuleReturns201WithLocationAndBody() throws Exception {
		when(createCategorisationRuleUseCase.create(any())).thenReturn(aRuleView(0L));

		mockMvc.perform(post("/api/v1/categorisation-rules")
						.with(adminToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(aCreateRequest())))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.matchValue").value("SHELL"))
				.andExpect(jsonPath("$.version").value(0));
	}

	@Test
	void createRuleReturnsBadRequestWhenPriorityIsNotPositive() throws Exception {
		CreateCategorisationRuleRequest request = new CreateCategorisationRuleRequest(CATEGORY_ID, "MERCHANT", "CONTAINS", "SHELL", "DEBIT", 0);

		mockMvc.perform(post("/api/v1/categorisation-rules")
						.with(adminToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("REQUEST_VALIDATION_FAILED"));
	}

	@Test
	void createRuleReturnsNotFoundWhenCategoryDoesNotExist() throws Exception {
		when(createCategorisationRuleUseCase.create(any())).thenThrow(new CategoryNotFoundException(CATEGORY_ID));

		mockMvc.perform(post("/api/v1/categorisation-rules")
						.with(adminToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(aCreateRequest())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("CATEGORY_NOT_FOUND"));
	}

	@Test
	void createRuleReturnsUnauthorizedWhenNoTokenIsSupplied() throws Exception {
		mockMvc.perform(post("/api/v1/categorisation-rules")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(aCreateRequest())))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void createRuleReturnsForbiddenWhenTheAuthorityIsMissing() throws Exception {
		mockMvc.perform(post("/api/v1/categorisation-rules")
						.with(jwt().jwt(builder -> builder.subject(ACTOR)).authorities(new SimpleGrantedAuthority("TRANSACTION_WRITE")))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(aCreateRequest())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
	}

	// --- rules: update ---

	@Test
	void updateRuleReturnsOkWithTheUpdatedBody() throws Exception {
		when(updateCategorisationRuleUseCase.update(eq(RULE_ID), any())).thenReturn(aRuleView(1L));

		mockMvc.perform(put("/api/v1/categorisation-rules/{id}", RULE_ID)
						.with(adminToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(anUpdateRequest())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.version").value(1));
	}

	@Test
	void updateRuleReturnsBadRequestWhenExpectedVersionIsMissing() throws Exception {
		String body = """
				{"categoryId":"%s","matchField":"MERCHANT","operator":"CONTAINS","matchValue":"SHELL","direction":"DEBIT","priority":30,"active":true}"""
				.formatted(CATEGORY_ID);

		mockMvc.perform(put("/api/v1/categorisation-rules/{id}", RULE_ID)
						.with(adminToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("REQUEST_VALIDATION_FAILED"));
	}

	@Test
	void updateRuleReturnsNotFoundWithRuleNotFound() throws Exception {
		when(updateCategorisationRuleUseCase.update(eq(RULE_ID), any()))
				.thenThrow(new RuleNotFoundException(RULE_ID));

		mockMvc.perform(put("/api/v1/categorisation-rules/{id}", RULE_ID)
						.with(adminToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(anUpdateRequest())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("RULE_NOT_FOUND"));
	}

	@Test
	void updateRuleReturnsConflictWithOptimisticLockConflict() throws Exception {
		when(updateCategorisationRuleUseCase.update(eq(RULE_ID), any()))
				.thenThrow(new RuleConflictException(RULE_ID, 0L, 1L));

		mockMvc.perform(put("/api/v1/categorisation-rules/{id}", RULE_ID)
						.with(adminToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(anUpdateRequest())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("OPTIMISTIC_LOCK_CONFLICT"));
	}

	@Test
	void updateRuleReturnsForbiddenWhenTheAuthorityIsMissing() throws Exception {
		mockMvc.perform(put("/api/v1/categorisation-rules/{id}", RULE_ID)
						.with(jwt().jwt(builder -> builder.subject(ACTOR)).authorities(new SimpleGrantedAuthority("TRANSACTION_WRITE")))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(anUpdateRequest())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
	}

}
