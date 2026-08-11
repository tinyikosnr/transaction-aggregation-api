package za.co.tinyiko.transactionaggregation.architecture;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import za.co.tinyiko.transactionaggregation.TransactionAggregationApiApplication;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the modular-monolith structure declared under
 * {@code za.co.tinyiko.transactionaggregation}.
 *
 * <p>{@link #moduleStructureIsValid()} enforces, in a single call: no dependency cycles
 * between modules, and no direct cross-module access to a module's internal (non-root)
 * packages. That second point covers both "no direct persistence access" and "controllers
 * cannot depend on repositories", since {@code api} and every {@code <module>.persistence}
 * package are distinct, non-exposed packages under Spring Modulith's default encapsulation.
 * It also covers {@code shared}'s explicit {@code allowedDependencies = {}} constraint
 * declared on its {@code package-info.java}.
 *
 * <p><strong>Not covered here:</strong> whether {@code domain} packages avoid depending on
 * Spring, Jakarta Persistence or presentation types. That is an intra-module layering rule,
 * not a cross-module boundary rule, and Spring Modulith's verification does not check it -
 * each module's own {@code <Module>ArchitectureTests} class covers that instead, via
 * {@link FrameworkIndependenceAssertions}.
 */
class ModularityTests {

	private static final ApplicationModules MODULES = ApplicationModules.of(TransactionAggregationApiApplication.class);

	private static final List<String> EXPECTED_MODULES = List.of(
			"api", "transaction", "categorisation", "aggregation",
			"customer", "merchant", "audit", "security", "config", "shared");

	@Test
	void discoversExpectedModules() {
		EXPECTED_MODULES.forEach(name -> assertThat(MODULES.getModuleByName(name))
				.as("module '%s' should be discovered", name)
				.isPresent());
	}

	@Test
	void moduleStructureIsValid() {
		MODULES.verify();
	}

}
