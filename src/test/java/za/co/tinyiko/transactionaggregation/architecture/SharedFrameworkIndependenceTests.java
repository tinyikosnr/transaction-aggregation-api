package za.co.tinyiko.transactionaggregation.architecture;

import java.util.List;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.shared.event.DomainEventEnvelope;
import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;

/**
 * Verifies that {@code shared}'s domain-neutral contracts stay framework-free - shared
 * abstractions should use plain Java/platform-neutral types, not framework types.
 */
class SharedFrameworkIndependenceTests {

	private static final List<Class<?>> SHARED_TYPES = List.of(
			DomainEventEnvelope.class,
			CorrelationId.class
	);

	@Test
	void sharedTypesDoNotReferenceSpring() {
		SHARED_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(type, "org.springframework"));
	}

	/**
	 * feature/structured-logging: no logging-framework facade in a business-rule-free shared
	 * abstraction either - mirrors the same "no Spring" independence this class already asserts.
	 */
	@Test
	void sharedTypesDoNotReferenceSlf4j() {
		SHARED_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(type, "org.slf4j"));
	}

}
