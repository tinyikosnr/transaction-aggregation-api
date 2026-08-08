package za.co.tinyiko.transactionaggregation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * <strong>Temporary placeholder, to be replaced by {@code feature/security}.</strong>
 * {@code spring-boot-starter-security} is on the classpath (added in {@code feature/project-structure}
 * for the not-yet-implemented JWT resource-server model), which means Spring Boot's default
 * security auto-configuration would otherwise require HTTP Basic auth with a random per-boot
 * generated password on every request, including this branch's own controller tests - there is
 * no authentication model implemented yet for a real one to check against.
 *
 * <p>This bean permits every request unconditionally and disables CSRF (a stateless, JWT-bearer
 * REST API with no browser session/cookie-based auth has no CSRF-relevant state to protect;
 * Spring's own reference documentation recommends disabling it for exactly this kind of API).
 * It deliberately does not enable HTTP Basic or form login - defining this bean at all replaces
 * Spring Boot's default security auto-configuration, so neither activates unless explicitly
 * configured, and neither is.
 *
 * <p>This is not the documented authorization model (ADR-016, TDS 43's endpoint authorization
 * matrix, {@code @PreAuthorize} on fine-grained authorities). Every endpoint in this branch is
 * unauthenticated. {@code feature/security} must replace this bean entirely.
 */
@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
		return http.build();
	}

}
