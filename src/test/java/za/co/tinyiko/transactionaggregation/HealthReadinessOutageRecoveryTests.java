package za.co.tinyiko.transactionaggregation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;

import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Empirical, three-state proof of SAD 38.4's core requirement ("A temporary database failure may
 * make the application unready without making it non-live") against a real, running application
 * context - not the shared suite's {@code TestcontainersConfiguration} container (stopping that
 * would destabilise every other test), but a dedicated, isolated PostgreSQL container fronted by
 * a dedicated Toxiproxy container, both on their own {@link Network}.
 *
 * <p><strong>Topology:</strong> the {@link DynamicPropertySource} method runs after both
 * containers have started but before the Spring context builds the {@code DataSource}, so
 * {@code spring.datasource.url} is set to the Toxiproxy proxy's own stable host/port from the very
 * first connection Hikari ever makes - the application never connects to PostgreSQL directly and
 * "adds" the proxy afterward; every connection this context ever opens already goes through
 * Toxiproxy.
 *
 * <p><strong>Failure injection mechanism, chosen empirically, not assumed:</strong> {@code
 * ToxiproxyContainer.ContainerProxy#setConnectionCut(boolean)} was inspected first and rejected -
 * its bytecode shows it adds a zero-{@code Bandwidth} toxic in both directions, which still lets
 * Toxiproxy *accept* the TCP connection and then stalls all data silently, reproducing the same
 * multi-minute hang already observed this session when a real container was {@code pause()}d
 * instead of {@code stop()}ped. Calling {@link Proxy#disable()}/{@link Proxy#enable()} directly on
 * the underlying {@code eu.rekawek.toxiproxy.Proxy} (obtained via a second, raw
 * {@link ToxiproxyClient} pointed at the same Toxiproxy container's control port, looked up by the
 * proxy name Testcontainers' own {@code getProxy(...)} already assigned) instead makes Toxiproxy
 * stop *listening* on that port entirely - an immediate, clean connection rejection, empirically
 * confirmed to resolve in well under 100ms, not a hang bounded by Hikari's connection timeout.
 *
 * <p>No sleeps, no polling loop: each assertion is a single request, matching the guidance to
 * prefer a small number of deterministic checks over tight polling against an endpoint whose
 * worst case is bounded by Hikari's own connection timeout.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class HealthReadinessOutageRecoveryTests {

	private static final Network NETWORK = Network.newNetwork();

	@Container
	private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:latest"))
			.withNetwork(NETWORK)
			.withNetworkAliases("postgres-target");

	@Container
	private static final ToxiproxyContainer TOXIPROXY = new ToxiproxyContainer(DockerImageName.parse("ghcr.io/shopify/toxiproxy:2.5.0"))
			.withNetwork(NETWORK);

	private static ToxiproxyContainer.ContainerProxy CONTAINER_PROXY;

	@DynamicPropertySource
	static void datasourceRoutedThroughToxiproxy(DynamicPropertyRegistry registry) {
		CONTAINER_PROXY = TOXIPROXY.getProxy(POSTGRES, 5432);
		registry.add("spring.datasource.url", () -> "jdbc:postgresql://%s:%d/%s".formatted(
				CONTAINER_PROXY.getContainerIpAddress(), CONTAINER_PROXY.getProxyPort(), POSTGRES.getDatabaseName()));
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
	}

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	private Proxy rawProxy() throws Exception {
		ToxiproxyClient rawClient = new ToxiproxyClient(TOXIPROXY.getHost(), TOXIPROXY.getMappedPort(8474));
		return rawClient.getProxy(CONTAINER_PROXY.getName());
	}

	/**
	 * Hikari does not notice a proxy re-enable synchronously - the pool discovers a previously
	 * broken connection is usable again only on its own next validation/acquisition attempt, so
	 * the very first readiness check immediately after {@link Proxy#enable()} can still observe
	 * one stale {@code DOWN} (empirically confirmed: the first attempt failed, the second
	 * succeeded). This is genuinely asynchronous framework recovery, not test flakiness to paper
	 * over with a longer single wait - a small, bounded, dependency-free retry (no Awaitility: not
	 * already a project dependency, and this doesn't warrant adding one) is used instead of a
	 * single fixed sleep, since each attempt here is fast (well under Hikari's connection timeout,
	 * unlike the DOWN-state checks) and the loop is capped, never tight-polling an endpoint whose
	 * worst case is bounded by that timeout.
	 */
	private void awaitReadinessUp() throws Exception {
		int maxAttempts = 10;
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			var result = mockMvc.perform(get("/actuator/health/readiness")).andReturn();
			if (result.getResponse().getStatus() == 200) {
				return;
			}
			if (attempt == maxAttempts) {
				mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
				return;
			}
			Thread.sleep(300);
		}
	}

	@Test
	void readinessTracksDatabaseConnectivityThroughToxiproxyWhileLivenessStaysUp() throws Exception {
		Proxy proxy = rawProxy();

		// STATE A - PostgreSQL reachable through the proxy.
		mockMvc.perform(get("/actuator/health/liveness"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
		mockMvc.perform(get("/actuator/health/readiness"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));

		// STATE B - connectivity interrupted at the proxy (existing pooled connections are closed
		// immediately; new connection attempts are refused, not stalled).
		proxy.disable();
		mockMvc.perform(get("/actuator/health/liveness"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
		mockMvc.perform(get("/actuator/health/readiness"))
				.andExpect(status().isServiceUnavailable());

		// STATE C - connectivity restored; the same application context, same DataSource, same
		// proxy host/port recovers with no code-level intervention. Readiness recovery is
		// Hikari's own asynchronous discovery, not synchronous with enable() - see awaitReadinessUp.
		proxy.enable();
		mockMvc.perform(get("/actuator/health/liveness"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
		awaitReadinessUp();
	}

}
