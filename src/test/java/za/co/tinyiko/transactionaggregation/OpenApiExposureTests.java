package za.co.tinyiko.transactionaggregation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Confirms the local-vs-default OpenAPI/Swagger UI exposure decision (CLAUDE.md, corrected plan
 * point B.2): both {@code /v3/api-docs} and Swagger UI are disabled under the default profile
 * (matching {@code application.properties}) and enabled under {@code local}
 * ({@code application-local.properties}), driven entirely by {@code springdoc.api-docs.enabled}/
 * {@code springdoc.swagger-ui.enabled} - not by this test overriding either property. Security
 * filters are left enabled throughout: the narrow {@code permitAll()} matchers in
 * {@code security.SecurityConfig} apply regardless of exposure, so an unauthenticated request
 * against a disabled path must reach Spring MVC and get a plain 404 (no handler registered), not a
 * 401 - see that class's own Javadoc for why a 401 there would be a (mild) information leak.
 */
@Import(TestcontainersConfiguration.class)
class OpenApiExposureTests {

	@Nested
	@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
	@AutoConfigureMockMvc
	class DefaultProfile {

		@Autowired
		private MockMvc mockMvc;

		@MockitoBean
		private JwtDecoder jwtDecoder;

		@Test
		void apiDocsIsNotExposed() throws Exception {
			mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
		}

		@Test
		void swaggerUiIsNotExposed() throws Exception {
			mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
		}
	}

	@Nested
	@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
	@AutoConfigureMockMvc
	@ActiveProfiles("local")
	class LocalProfile {

		@Autowired
		private MockMvc mockMvc;

		@Test
		void apiDocsIsExposed() throws Exception {
			mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
		}

		@Test
		void swaggerUiIsExposed() throws Exception {
			mockMvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
			mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
		}
	}

}
