package za.co.tinyiko.transactionaggregation.security;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class RoleClaimAuthoritiesConverterTests {

	private final RoleClaimAuthoritiesConverter converter = new RoleClaimAuthoritiesConverter();

	private static Jwt jwtWithRoles(List<String> roles) {
		Jwt.Builder builder = Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject("test-subject")
				.issuedAt(Instant.parse("2026-01-01T00:00:00Z"))
				.expiresAt(Instant.parse("2026-01-01T01:00:00Z"));
		if (roles != null) {
			builder.claim("roles", roles);
		}
		return builder.build();
	}

	private static List<String> authorityNames(Collection<GrantedAuthority> authorities) {
		return authorities.stream().map(GrantedAuthority::getAuthority).toList();
	}

	@Test
	void expandsRoleApiConsumerToItsApprovedAuthorities() {
		Collection<GrantedAuthority> authorities = converter.convert(jwtWithRoles(List.of("ROLE_API_CONSUMER")));

		assertThat(authorityNames(authorities)).containsExactlyInAnyOrder(
				"TRANSACTION_READ", "TRANSACTION_WRITE", "AGGREGATION_READ");
	}

	@Test
	void expandsRoleSupportToItsApprovedAuthorities() {
		Collection<GrantedAuthority> authorities = converter.convert(jwtWithRoles(List.of("ROLE_SUPPORT")));

		assertThat(authorityNames(authorities)).containsExactlyInAnyOrder(
				"TRANSACTION_READ", "CUSTOMER_READ", "AGGREGATION_READ");
	}

	@Test
	void expandsRoleAdminToItsApprovedAuthorities() {
		Collection<GrantedAuthority> authorities = converter.convert(jwtWithRoles(List.of("ROLE_ADMIN")));

		assertThat(authorityNames(authorities)).containsExactlyInAnyOrder(
				"TRANSACTION_READ", "TRANSACTION_WRITE", "CUSTOMER_READ", "AGGREGATION_READ",
				"CATEGORY_ADMIN", "AUDIT_READ", "OPERATIONS_READ");
	}

	@Test
	void unionsAuthoritiesWhenMultipleRolesArePresent() {
		Collection<GrantedAuthority> authorities = converter.convert(jwtWithRoles(List.of("ROLE_SUPPORT", "ROLE_API_CONSUMER")));

		assertThat(authorityNames(authorities)).containsExactlyInAnyOrder(
				"TRANSACTION_READ", "TRANSACTION_WRITE", "CUSTOMER_READ", "AGGREGATION_READ");
	}

	@Test
	void ignoresAnUnrecognisedRoleWithoutFailing() {
		Collection<GrantedAuthority> authorities = converter.convert(jwtWithRoles(List.of("ROLE_UNKNOWN", "ROLE_SUPPORT")));

		assertThat(authorityNames(authorities)).containsExactlyInAnyOrder("TRANSACTION_READ", "CUSTOMER_READ", "AGGREGATION_READ");
	}

	@Test
	void returnsNoAuthoritiesWhenRolesClaimIsMissing() {
		Collection<GrantedAuthority> authorities = converter.convert(jwtWithRoles(null));

		assertThat(authorities).isEmpty();
	}

}
