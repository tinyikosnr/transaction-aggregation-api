package za.co.tinyiko.transactionaggregation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * The OpenAPI document root (SAD 35.9). {@code version} is the literal external API contract
 * version ({@code "v1"}, matching the {@code /api/v1} path prefix every controller uses), not the
 * Maven {@code 0.0.1-SNAPSHOT} artifact version - the two version a different thing and have no
 * reason to be coupled. Contact/license/externalDocs/fixed server URLs are deliberately omitted:
 * none are documented, and inventing placeholder values would misrepresent the API.
 *
 * <p>One HTTP bearer/JWT {@link SecurityScheme}, applied globally via
 * {@link OpenAPI#addSecurityItem(SecurityRequirement)} rather than per-operation
 * {@code @SecurityRequirement}, since every business endpoint in this API requires authentication
 * (see {@code security.SecurityConfig}'s {@code anyRequest().authenticated()}). No OAuth2 flows or
 * authorization/token URLs are declared - this API validates bearer JWTs issued by an external
 * identity provider, it does not itself issue them, and the five fine-grained authorities
 * ({@code TRANSACTION_READ}, {@code TRANSACTION_WRITE}, {@code AGGREGATION_READ},
 * {@code CATEGORY_ADMIN}, {@code AUDIT_READ}) are plain Spring {@code @PreAuthorize} authorities,
 * documented in each operation's description, never modelled as OAuth2 scopes.
 */
@Configuration
class OpenApiConfig {

	private static final String BEARER_SECURITY_SCHEME = "bearerAuth";

	@Bean
	OpenAPI transactionAggregationOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Transaction Aggregation API")
						.version("v1"))
				.components(new Components()
						.addSecuritySchemes(BEARER_SECURITY_SCHEME, new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList(BEARER_SECURITY_SCHEME));
	}

}
