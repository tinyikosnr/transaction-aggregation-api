package za.co.tinyiko.transactionaggregation.architecture;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shared reflection-based check used by the architecture tests to assert that a given type's
 * superclass, interfaces, field types and method signatures never reference a forbidden
 * package. Not ArchUnit: Spring Modulith's {@code ApplicationModules.verify()} only checks
 * cross-module boundaries, not whether a class imports a particular framework package, so this
 * fills that one gap. Proportionate while the set of types needing this check stays small and
 * simple — see {@code CLAUDE.md} for when to reconsider ArchUnit instead.
 */
final class FrameworkIndependenceAssertions {

	private FrameworkIndependenceAssertions() {
	}

	static void assertNoForbiddenReference(Class<?> type, String... forbiddenPackagePrefixes) {
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
				.filter(name -> Arrays.stream(forbiddenPackagePrefixes).anyMatch(name::startsWith))
				.toList();

		assertThat(violations)
				.as("%s must not reference types under %s", type.getName(), Arrays.toString(forbiddenPackagePrefixes))
				.isEmpty();
	}

}
