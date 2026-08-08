package za.co.tinyiko.transactionaggregation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TransactionAggregationApiApplicationTests {

	/**
	 * {@code security.SecurityConfig}'s default-profile {@code JwtDecoder} bean requires
	 * {@code JWT_ISSUER_URI} to be set (deliberately - see its own Javadoc), which is not, and
	 * should not be, set in a test environment. Mocked here purely so this otherwise-unrelated
	 * context-loads smoke test doesn't depend on it.
	 */
	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void contextLoads() {
	}

}
