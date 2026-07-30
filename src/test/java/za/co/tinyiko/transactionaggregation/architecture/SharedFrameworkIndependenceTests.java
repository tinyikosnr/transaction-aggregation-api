package za.co.tinyiko.transactionaggregation.architecture;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.shared.event.DomainEventEnvelope;
import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@code shared}'s domain-neutral contracts stay framework-free, per
 * {@code CLAUDE.md}'s "prefer Java types in shared abstractions" rule.
 *
 * <p>Spring Modulith's {@code ApplicationModules.verify()} (see {@link ModularityTests}) checks
 * cross-module boundaries; it has no concept of "does this class reference Spring." This is a
 * deliberately small, hand-rolled reflection check instead of ArchUnit: the set of shared types
 * is currently two known, simple records, so a targeted check is proportionate. Revisit ArchUnit
 * if/when this list grows large or varied enough that a hand-rolled check becomes unwieldy.
 */
class SharedFrameworkIndependenceTests {

	private static final List<Class<?>> SHARED_TYPES = List.of(
			DomainEventEnvelope.class,
			CorrelationId.class
	);

	@Test
	void sharedTypesDoNotReferenceSpring() {
		SHARED_TYPES.forEach(SharedFrameworkIndependenceTests::assertNoSpringReference);
	}

	private static void assertNoSpringReference(Class<?> type) {
		List<Class<?>> referencedTypes = new ArrayList<>();
		referencedTypes.add(type.getSuperclass());
		referencedTypes.addAll(List.of(type.getInterfaces()));

		for (Field field : type.getDeclaredFields()) {
			referencedTypes.add(field.getType());
		}
		for (Method method : type.getDeclaredMethods()) {
			referencedTypes.add(method.getReturnType());
			referencedTypes.addAll(List.of(method.getParameterTypes()));
		}

		List<String> violations = referencedTypes.stream()
				.filter(Objects::nonNull)
				.map(Class::getName)
				.filter(name -> name.startsWith("org.springframework"))
				.toList();

		assertThat(violations)
				.as("%s must not reference Spring types", type.getName())
				.isEmpty();
	}

}
