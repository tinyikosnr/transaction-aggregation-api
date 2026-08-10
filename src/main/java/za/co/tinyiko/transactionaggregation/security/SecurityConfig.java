package za.co.tinyiko.transactionaggregation.security;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import tools.jackson.databind.ObjectMapper;

/**
 * The real, permanent security model (SAD 36, TDS 41-44, ADR-007, ADR-016), replacing the
 * temporary permit-all configuration that lived in {@code config.SecurityConfig} during
 * {@code feature/api}. Moved to {@code security}: this module's own {@code package-info.java}
 * already documented "JWT validation, authority/role extraction, and access-denied handling" as
 * its responsibility before any of it existed - this class and its package-private collaborators
 * ({@link RoleClaimAuthoritiesConverter}, {@link ProblemDetailAuthenticationEntryPoint},
 * {@link ProblemDetailAccessDeniedHandler}) are that responsibility, finally implemented. None of
 * those three need to be Spring beans in their own right - they have no dependencies Spring needs
 * to inject beyond the already-available {@link ObjectMapper}, so this class constructs them
 * directly, the same way a module's own package-private {@code @Service} is never reached for
 * except through the one public seam it needs.
 *
 * <p>Stateless bearer-token REST API: CSRF disabled (no cookie-based session to protect - TDS 44),
 * no CORS configuration (SAD 36.12: disabled by default, nothing documents a browser client), no
 * HTTP Basic or form login (defining this bean replaces Boot's default security auto-configuration
 * entirely, so neither activates by omission - the same mechanism the old placeholder relied on).
 *
 * <p>{@code @PreAuthorize} (activated via {@link EnableMethodSecurity}) is the single source of
 * authorization truth for fine-grained authorities (SAD 36.4; CLAUDE.md's own pre-committed
 * convention, "documentation only" until now). {@code authorizeHttpRequests} below only
 * distinguishes public from authenticated - it never repeats an authority check.
 *
 * <p>The OpenAPI/Swagger UI paths ({@code /v3/api-docs}, {@code /v3/api-docs.yaml},
 * {@code /v3/api-docs/**}, {@code /swagger-ui.html}, {@code /swagger-ui/**} - the exact set
 * springdoc-openapi 3.0.3 registers, confirmed empirically rather than assumed from its docs) are
 * {@code permitAll()} unconditionally, the same as {@code /actuator/health}: whether they actually
 * serve anything is controlled per-profile by {@code springdoc.api-docs.enabled}/
 * {@code springdoc.swagger-ui.enabled} (disabled by default/production, enabled under
 * {@code local} - see {@code application.properties}/{@code application-local.properties}), not by
 * this filter chain. Leaving them {@code permitAll()} even when disabled means a request against a
 * disabled path gets a plain 404 (no matching handler) rather than a 401 that would otherwise leak
 * "this path exists but you're not authenticated for it".
 *
 * <p>{@code /actuator/prometheus}/{@code /actuator/metrics}/{@code /actuator/metrics/**}
 * (feature/observability) require the {@code OPERATIONS_READ} authority (SAD 36.4: "Access
 * selected operational endpoints"; TDS 43: {@code /actuator/metrics} is Admin-only) - the same,
 * already-mapped authority {@code RoleClaimAuthoritiesConverter} grants to {@code ROLE_ADMIN},
 * not a newly invented one. Deliberately no second, separately-ordered {@code SecurityFilterChain}
 * for actuator paths (TDS 67 names a distinct {@code ActuatorSecurityConfig} class, but this
 * project's own {@code SecurityConfig} already owns every other path-level authorization rule,
 * including the OpenAPI paths above - adding a second filter chain here would be the one
 * inconsistent pattern relative to that). {@code /actuator/health} stays {@code permitAll()},
 * unchanged.
 *
 * <p>The custom {@code AuthenticationEntryPoint} is registered through
 * {@code oauth2ResourceServer(oauth2 -> oauth2.authenticationEntryPoint(...))}, not the generic
 * {@code exceptionHandling(...).authenticationEntryPoint(...)} - found empirically, not assumed:
 * {@code OAuth2ResourceServerConfigurer} registers its own {@code BearerTokenAuthenticationEntryPoint}
 * for bearer-token failures, which took precedence over a generically-registered one in testing
 * (a 401 came back with Spring's own default {@code WWW-Authenticate} header and an empty body,
 * not this class's {@link org.springframework.http.ProblemDetail} response). The
 * {@code AccessDeniedHandler} has no equivalent resource-server-specific hook, so it stays on the
 * generic {@code exceptionHandling(...)} DSL.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder, ObjectMapper objectMapper) throws Exception {
		JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
		jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new RoleClaimAuthoritiesConverter());

		http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/actuator/health").permitAll()
						.requestMatchers("/v3/api-docs", "/v3/api-docs.yaml", "/v3/api-docs/**",
								"/swagger-ui.html", "/swagger-ui/**").permitAll()
						.requestMatchers("/actuator/prometheus", "/actuator/metrics", "/actuator/metrics/**")
								.hasAuthority("OPERATIONS_READ")
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter))
						.authenticationEntryPoint(new ProblemDetailAuthenticationEntryPoint(objectMapper)))
				.exceptionHandling(exceptions -> exceptions
						.accessDeniedHandler(new ProblemDetailAccessDeniedHandler(objectMapper)));

		return http.build();
	}

	/**
	 * Production/default {@link JwtDecoder}: validates signature (RS256-restricted by
	 * {@code NimbusJwtDecoder}'s own default when built from a JWK set - SAD 36.3's "must not
	 * accept the algorithm from the token without validating it against configured trusted
	 * algorithms" requires no extra code as a result), issuer, expiry (Spring's default 60s clock
	 * skew - no override is documented, so the default is used rather than an invented value), and
	 * audience (added explicitly - Spring's default validator chain does not check {@code aud} on
	 * its own, despite SAD 36.3 requiring it; easy to miss).
	 *
	 * <p>{@code issuer-uri} has no fallback default: an unset {@code JWT_ISSUER_URI} must fail
	 * application startup outright, rather than silently accept tokens from any/no issuer.
	 */
	@Bean
	@Profile("!local")
	JwtDecoder jwtDecoder(
			@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
			@Value("${app.security.expected-audience:transaction-aggregation-api}") String expectedAudience
	) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();

		OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
		OAuth2TokenValidator<Jwt> withAudience = new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
				audiences -> audiences != null && audiences.contains(expectedAudience));
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));

		return decoder;
	}

	/**
	 * Local-development-only {@link JwtDecoder}: a symmetric key, so a developer can mint a test
	 * token locally (see README) without standing up a real identity provider. Never active outside
	 * the {@code local} profile (SAD 44.2's own documented profile list); the key is a clearly-fake,
	 * published-in-source placeholder, not a secret - it must never be used, and could not be
	 * meaningfully used, outside a developer's own machine.
	 */
	@Bean
	@Profile("local")
	JwtDecoder localJwtDecoder() {
		SecretKeySpec key = new SecretKeySpec(
				"local-only-test-signing-key-not-a-real-secret-32bytes-minimum".getBytes(StandardCharsets.UTF_8),
				"HmacSHA256");
		return NimbusJwtDecoder.withSecretKey(key).build();
	}

}
