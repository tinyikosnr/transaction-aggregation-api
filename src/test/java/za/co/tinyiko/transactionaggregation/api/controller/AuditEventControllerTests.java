package za.co.tinyiko.transactionaggregation.api.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import za.co.tinyiko.transactionaggregation.audit.application.AuditEventSearchResult;
import za.co.tinyiko.transactionaggregation.audit.application.AuditEventView;
import za.co.tinyiko.transactionaggregation.audit.application.AuditSearchValidationException;
import za.co.tinyiko.transactionaggregation.audit.application.SearchAuditEventsUseCase;
import za.co.tinyiko.transactionaggregation.security.SecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditEventController.class)
@Import(SecurityConfig.class)
class AuditEventControllerTests {

	private static final String ACTOR = "jwt-subject-001";
	private static final UUID AGGREGATE_ID = UUID.randomUUID();

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SearchAuditEventsUseCase searchAuditEventsUseCase;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	private static RequestPostProcessor auditReadToken() {
		return jwt().jwt(builder -> builder.subject(ACTOR)).authorities(new SimpleGrantedAuthority("AUDIT_READ"));
	}

	private static AuditEventView aView() {
		return new AuditEventView(UUID.randomUUID(), "TRANSACTION", AGGREGATE_ID, "TRANSACTION_CREATED",
				"api-consumer-1", "corr-1", "{\"foo\":\"bar\"}", Instant.parse("2026-08-15T08:00:00Z"));
	}

	@Test
	void searchReturnsOkWithTheMappedResultAndNestedEventData() throws Exception {
		AuditEventSearchResult result = new AuditEventSearchResult(List.of(aView()), 0, 20, 1, 1);
		when(searchAuditEventsUseCase.search(any())).thenReturn(result);

		mockMvc.perform(get("/api/v1/audit-events").with(auditReadToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].eventType").value("TRANSACTION_CREATED"))
				.andExpect(jsonPath("$.content[0].eventData.foo").value("bar"))
				.andExpect(jsonPath("$.page.number").value(0))
				.andExpect(jsonPath("$.page.size").value(20))
				.andExpect(jsonPath("$.page.totalElements").value(1))
				.andExpect(jsonPath("$.page.totalPages").value(1));
	}

	@Test
	void searchPassesQueryParametersThroughToTheCriteria() throws Exception {
		when(searchAuditEventsUseCase.search(any())).thenReturn(new AuditEventSearchResult(List.of(), 1, 10, 0, 0));

		mockMvc.perform(get("/api/v1/audit-events")
						.with(auditReadToken())
						.param("aggregateType", "TRANSACTION")
						.param("aggregateId", AGGREGATE_ID.toString())
						.param("eventType", "TRANSACTION_CREATED")
						.param("actor", "api-consumer-1")
						.param("correlationId", "corr-1")
						.param("occurredFrom", "2026-01-01T00:00:00Z")
						.param("occurredTo", "2026-01-31T23:59:59Z")
						.param("page", "1")
						.param("size", "10")
						.param("sort", "occurredAt,asc"))
				.andExpect(status().isOk());
	}

	@Test
	void searchReturnsBadRequestWhenTheUseCaseSignalsAValidationFailure() throws Exception {
		when(searchAuditEventsUseCase.search(any()))
				.thenThrow(new AuditSearchValidationException("aggregateType is required when aggregateId is supplied"));

		mockMvc.perform(get("/api/v1/audit-events").with(auditReadToken()).param("aggregateId", AGGREGATE_ID.toString()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("REQUEST_VALIDATION_FAILED"));
	}

	@Test
	void searchReturnsUnauthorizedWhenNoTokenIsSupplied() throws Exception {
		mockMvc.perform(get("/api/v1/audit-events"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	void searchReturnsForbiddenWithTransactionReadAuthorityAlone() throws Exception {
		mockMvc.perform(get("/api/v1/audit-events")
						.with(jwt().jwt(builder -> builder.subject(ACTOR)).authorities(new SimpleGrantedAuthority("TRANSACTION_READ"))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
	}

	@Test
	void searchReturnsForbiddenWithCategoryAdminAuthorityAlone() throws Exception {
		mockMvc.perform(get("/api/v1/audit-events")
						.with(jwt().jwt(builder -> builder.subject(ACTOR)).authorities(new SimpleGrantedAuthority("CATEGORY_ADMIN"))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
	}

	@Test
	void searchReturnsOkWithEmptyResultShape() throws Exception {
		when(searchAuditEventsUseCase.search(any())).thenReturn(new AuditEventSearchResult(List.of(), 0, 20, 0, 0));

		mockMvc.perform(get("/api/v1/audit-events").with(auditReadToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isEmpty())
				.andExpect(jsonPath("$.page.totalElements").value(0));
	}

}
