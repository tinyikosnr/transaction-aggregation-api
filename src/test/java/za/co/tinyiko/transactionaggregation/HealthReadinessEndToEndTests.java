package za.co.tinyiko.transactionaggregation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Normal-state and security coverage for the health/readiness endpoints (feature/health-readiness,
 * SAD 38.4/36.10), against the real, shared Testcontainers PostgreSQL instance - no outage
 * simulation here, see {@link HealthReadinessOutageRecoveryTests} for that (a genuinely different
 * infrastructure topology, kept in its own file for exactly that reason, the same way
 * {@code JpaCategorisationRuleRepositoryAdapterConcurrencyTests} is kept separate from its own
 * sibling adapter test).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class HealthReadinessEndToEndTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void healthIsPubliclyAccessible() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void livenessIsPubliclyAccessibleAndUp() throws Exception {
		mockMvc.perform(get("/actuator/health/liveness"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void readinessIsPubliclyAccessibleAndUpWhenPostgresIsReachable() throws Exception {
		mockMvc.perform(get("/actuator/health/readiness"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	/**
	 * Public probe responses stay minimal-detail - no database vendor/connection information is
	 * disclosed anonymously, confirmed directly rather than assumed from
	 * {@code management.endpoint.health.show-details}'s default.
	 */
	@Test
	void readinessResponseExposesNoComponentDetail() throws Exception {
		mockMvc.perform(get("/actuator/health/readiness"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.components").doesNotExist())
				.andExpect(jsonPath("$.details").doesNotExist());
	}

	/**
	 * Regression guard: metrics/Prometheus security (feature/observability) must remain untouched
	 * by this branch's health matcher additions.
	 */
	@Test
	void metricsAndPrometheusStillRequireOperationsReadAuthority() throws Exception {
		mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());

		mockMvc.perform(get("/actuator/metrics")
						.with(jwt().jwt(b -> b.subject("x")).authorities(new SimpleGrantedAuthority("OPERATIONS_READ"))))
				.andExpect(status().isOk());
		mockMvc.perform(get("/actuator/prometheus")
						.with(jwt().jwt(b -> b.subject("x")).authorities(new SimpleGrantedAuthority("OPERATIONS_READ"))))
				.andExpect(status().isOk());
	}

}
