package za.co.tinyiko.transactionaggregation.architecture;

import java.util.List;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.shared.event.DomainEventEnvelope;
import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;

/**
 * Verifies that {@code shared}'s domain-neutral contracts stay framework-free, per
 * {@code CLAUDE.md}'s "prefer Java types in shared abstractions" rule.
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

}
