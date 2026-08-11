package za.co.tinyiko.transactionaggregation.architecture;

import java.util.List;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.customer.application.CustomerExistsPort;
import za.co.tinyiko.transactionaggregation.customer.application.CustomerLookupPort;
import za.co.tinyiko.transactionaggregation.customer.application.CustomerNotFoundException;
import za.co.tinyiko.transactionaggregation.customer.application.DuplicateCustomerReferenceException;
import za.co.tinyiko.transactionaggregation.customer.application.RegisterCustomerCommand;
import za.co.tinyiko.transactionaggregation.customer.application.RegisterCustomerUseCase;
import za.co.tinyiko.transactionaggregation.customer.domain.Customer;
import za.co.tinyiko.transactionaggregation.customer.domain.CustomerId;
import za.co.tinyiko.transactionaggregation.customer.domain.CustomerStatus;

/**
 * The first real domain module's architecture checks. Two rules, neither expressible through
 * Spring Modulith's {@code ApplicationModules.verify()} (see {@link ModularityTests}), which
 * only checks cross-module boundaries:
 *
 * <ul>
 *     <li>{@code customer.domain} must not depend on Spring or Jakarta Persistence — an
 *     intra-module layering rule, not a cross-module one.</li>
 *     <li>{@code customer.application} must not depend on {@code customer.persistence} — the
 *     application layer must go through {@code CustomerRepositoryPort}, never the JPA types
 *     directly.</li>
 * </ul>
 *
 * <p>Still no ArchUnit: this is a direct, proportionate extension of the same hand-rolled
 * reflection check already used for {@code shared} — two rules, six known classes. Revisit
 * ArchUnit if this keeps growing across modules to the point of real duplication, or if a rule
 * comes up that reflection can't express cleanly.
 */
class CustomerArchitectureTests {

	private static final List<Class<?>> DOMAIN_TYPES = List.of(
			Customer.class,
			CustomerId.class,
			CustomerStatus.class
	);

	private static final List<Class<?>> APPLICATION_TYPES = List.of(
			CustomerLookupPort.class,
			CustomerExistsPort.class,
			RegisterCustomerUseCase.class,
			RegisterCustomerCommand.class,
			CustomerNotFoundException.class,
			DuplicateCustomerReferenceException.class
	);

	@Test
	void domainDoesNotReferenceFrameworkTypes() {
		DOMAIN_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(
				type, "org.springframework", "jakarta.persistence", "org.slf4j"));
	}

	@Test
	void applicationDoesNotReferencePersistence() {
		APPLICATION_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(
				type, "za.co.tinyiko.transactionaggregation.customer.persistence"));
	}

}
