package za.co.tinyiko.transactionaggregation.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the single injectable {@link Clock} the application uses for deterministic,
 * testable timestamp generation — added here, minimally, because {@code customer}'s first
 * {@code @Service} bean needs one to be constructible. No {@code ClockProvider} wrapper: this
 * is exactly what {@code java.time.Clock} already is.
 */
@Configuration
class ClockConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
