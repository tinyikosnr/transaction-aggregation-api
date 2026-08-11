package za.co.tinyiko.transactionaggregation;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Targeted structural assertions against the generated {@code /v3/api-docs} document
 * (feature/openapi-documentation) - not a full snapshot, per the explicit instruction to avoid
 * brittle whole-document comparisons. {@code springdoc.api-docs.enabled} is force-enabled here via
 * {@link TestPropertySource} regardless of the active profile's own default, since this test's
 * purpose is document content, not exposure policy - see {@link OpenApiExposureTests} for that.
 * Security filters are left enabled (unlike the earlier throwaway compatibility-gate test) to
 * prove the narrow {@code security.SecurityConfig} matchers genuinely permit an unauthenticated
 * request through to the document.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = "springdoc.api-docs.enabled=true")
class OpenApiDocumentStructureTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void documentHasTheExpectedInfoBlock() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.openapi").exists())
				.andExpect(jsonPath("$.info.title").value("Transaction Aggregation API"))
				.andExpect(jsonPath("$.info.version").value("v1"));
	}

	@Test
	void documentDeclaresTheBearerSecurityScheme() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
				.andExpect(jsonPath("$.security[0].bearerAuth").exists());
	}

	@Test
	void documentCoversEveryBusinessEndpoint() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/v1/transactions'].post").exists())
				.andExpect(jsonPath("$.paths['/api/v1/transactions'].get").exists())
				.andExpect(jsonPath("$.paths['/api/v1/transactions/bulk'].post").exists())
				.andExpect(jsonPath("$.paths['/api/v1/transactions/{id}'].get").exists())
				.andExpect(jsonPath("$.paths['/api/v1/customers/{customerId}/summary'].get").exists())
				.andExpect(jsonPath("$.paths['/api/v1/customers/{customerId}/categories'].get").exists())
				.andExpect(jsonPath("$.paths['/api/v1/customers/{customerId}/merchants'].get").exists())
				.andExpect(jsonPath("$.paths['/api/v1/customers/{customerId}/monthly-summary'].get").exists())
				.andExpect(jsonPath("$.paths['/api/v1/categories'].get").exists())
				.andExpect(jsonPath("$.paths['/api/v1/categories/{id}'].get").exists())
				.andExpect(jsonPath("$.paths['/api/v1/categorisation-rules'].get").exists())
				.andExpect(jsonPath("$.paths['/api/v1/categorisation-rules'].post").exists())
				.andExpect(jsonPath("$.paths['/api/v1/categorisation-rules/{id}'].get").exists())
				.andExpect(jsonPath("$.paths['/api/v1/categorisation-rules/{id}'].put").exists())
				.andExpect(jsonPath("$.paths['/api/v1/audit-events'].get").exists());
	}

	@Test
	void documentTagsMatchTheFourDocumentedGroups() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tags[?(@.name=='Transactions')]").exists())
				.andExpect(jsonPath("$.tags[?(@.name=='Aggregation')]").exists())
				.andExpect(jsonPath("$.tags[?(@.name=='Category Administration')]").exists())
				.andExpect(jsonPath("$.tags[?(@.name=='Audit')]").exists());
	}

	/**
	 * {@code eventData} is declared {@code implementation = Object.class} (rather than
	 * {@code type = "object"} alone), which swagger-core renders as an unconstrained "any value"
	 * schema (no {@code type} keyword at all - valid OpenAPI 3.1/JSON Schema for "any type") rather
	 * than a literal {@code "type": "object"}: found empirically, not assumed - swagger-core's
	 * Jackson-2-based introspection does not recognise {@code tools.jackson.databind.JsonNode}
	 * (this codebase's Jackson 3 type) and otherwise falls back to treating it as a string, which
	 * this test guards against instead. The genuinely load-bearing guarantee - that the runtime
	 * response embeds {@code eventData} as a real nested JSON object, not a re-escaped string - is
	 * already covered by {@code AuditEventControllerTests}; this test only guards the documentation
	 * from regressing to the misleading "string" schema.
	 */
	@Test
	void auditEventDataSchemaIsNotMisdocumentedAsAString() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.components.schemas.AuditEventResponse.properties.eventData.type").doesNotExist());
	}

	/**
	 * Generic, not a hard-coded per-path list (feature/health-readiness): asserts no generated
	 * path starts with {@code /actuator/} at all, so a future actuator endpoint (liveness,
	 * readiness, or anything added later) is covered automatically without another one-off edit
	 * here - the same reasoning {@code OpenApiArchitectureTests} already applies at the annotation
	 * level, extended to the generated document's own path list.
	 */
	@Test
	void noGeneratedPathStartsWithActuator() throws Exception {
		String body = mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		JsonNode paths = objectMapper.readTree(body).get("paths");
		List<String> actuatorPaths = paths.propertyNames().stream()
				.filter(path -> path.startsWith("/actuator/"))
				.toList();

		assertThat(actuatorPaths).as("no generated OpenAPI path should start with /actuator/").isEmpty();
	}

}
