package za.co.tinyiko.transactionaggregation.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Expands a JWT's {@code roles} claim into Spring Security {@link GrantedAuthority} instances,
 * per the approved role-to-authority mapping (ADR-016, TDS 42, SAD 36.4/49.1) - the exact three
 * roles and their exact authority sets, nothing invented.
 *
 * <p>Claim values are already {@code ROLE_}-prefixed in the token (TDS 41's example JWT:
 * {@code "roles": ["ROLE_API_CONSUMER"]}), so this does not add its own prefix. Multiple roles in
 * the claim are unioned. An unrecognised role value contributes no authorities rather than
 * throwing - a client with an undocumented role simply gets nothing from it, without breaking
 * parsing of the rest of the claim. A missing {@code roles} claim yields zero authorities (fails
 * closed: every {@code @PreAuthorize} check then denies, rather than granting anything by
 * default).
 *
 * <p>Not Spring's default {@code scope}/{@code scp}-based {@code JwtGrantedAuthoritiesConverter}
 * with its {@code SCOPE_} prefix - that reads a different claim than the one this API's JWTs
 * actually carry.
 */
class RoleClaimAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	private static final String ROLES_CLAIM = "roles";

	private static final Map<String, Set<String>> ROLE_AUTHORITIES = Map.of(
			"ROLE_API_CONSUMER", Set.of("TRANSACTION_READ", "TRANSACTION_WRITE", "AGGREGATION_READ"),
			"ROLE_SUPPORT", Set.of("TRANSACTION_READ", "CUSTOMER_READ", "AGGREGATION_READ"),
			"ROLE_ADMIN", Set.of("TRANSACTION_READ", "TRANSACTION_WRITE", "CUSTOMER_READ", "AGGREGATION_READ",
					"CATEGORY_ADMIN", "AUDIT_READ", "OPERATIONS_READ")
	);

	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		List<String> roles = jwt.getClaimAsStringList(ROLES_CLAIM);
		if (roles == null) {
			return Set.of();
		}
		return roles.stream()
				.map(role -> ROLE_AUTHORITIES.getOrDefault(role, Set.of()))
				.flatMap(Set::stream)
				.map(SimpleGrantedAuthority::new)
				.collect(Collectors.toUnmodifiableSet());
	}

}
