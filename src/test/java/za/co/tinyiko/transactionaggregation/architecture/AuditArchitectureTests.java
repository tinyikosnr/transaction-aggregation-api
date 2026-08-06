package za.co.tinyiko.transactionaggregation.architecture;

import java.util.List;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.audit.application.RecordAuditEventCommand;
import za.co.tinyiko.transactionaggregation.audit.application.RecordAuditEventUseCase;
import za.co.tinyiko.transactionaggregation.audit.domain.AuditEvent;
import za.co.tinyiko.transactionaggregation.audit.domain.AuditEventId;

/**
 * Same two rules as {@link CategorisationArchitectureTests} and the other module architecture
 * tests, applied to {@code audit}. {@code AuditService} is package-private so it is not
 * importable from this package, and is therefore excluded from {@code APPLICATION_TYPES}.
 */
class AuditArchitectureTests {

	private static final List<Class<?>> DOMAIN_TYPES = List.of(
			AuditEvent.class,
			AuditEventId.class
	);

	private static final List<Class<?>> APPLICATION_TYPES = List.of(
			RecordAuditEventUseCase.class,
			RecordAuditEventCommand.class
	);

	@Test
	void domainDoesNotReferenceFrameworkTypes() {
		DOMAIN_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(
				type, "org.springframework", "jakarta.persistence"));
	}

	@Test
	void applicationDoesNotReferencePersistence() {
		APPLICATION_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(
				type, "za.co.tinyiko.transactionaggregation.audit.persistence"));
	}

}
