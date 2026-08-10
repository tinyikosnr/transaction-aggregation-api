package za.co.tinyiko.transactionaggregation.architecture;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.audit.application.AuditEventSearchCriteria;
import za.co.tinyiko.transactionaggregation.audit.application.AuditEventSearchResult;
import za.co.tinyiko.transactionaggregation.audit.application.AuditEventView;
import za.co.tinyiko.transactionaggregation.audit.application.AuditSearchValidationException;
import za.co.tinyiko.transactionaggregation.audit.application.RecordAuditEventCommand;
import za.co.tinyiko.transactionaggregation.audit.application.RecordAuditEventUseCase;
import za.co.tinyiko.transactionaggregation.audit.application.SearchAuditEventsUseCase;
import za.co.tinyiko.transactionaggregation.audit.domain.AuditEvent;
import za.co.tinyiko.transactionaggregation.audit.domain.AuditEventId;
import za.co.tinyiko.transactionaggregation.audit.port.AuditEventRow;
import za.co.tinyiko.transactionaggregation.audit.port.AuditEventSearchPage;
import za.co.tinyiko.transactionaggregation.audit.port.AuditEventSearchQuery;
import za.co.tinyiko.transactionaggregation.audit.port.AuditQueryRepositoryPort;
import za.co.tinyiko.transactionaggregation.audit.port.AuditRepositoryPort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Same two rules as every other module's architecture tests, applied to {@code audit}, plus a
 * third (feature/audit-query) that makes the write/read port separation a build-time-verified
 * fact rather than just a design intention: {@link AuditQueryRepositoryPort} must expose no
 * method whose name suggests a mutation, and {@link AuditRepositoryPort} (the write side) must
 * stay completely untouched by this branch. {@code AuditService}/{@code SearchAuditEventsService}
 * are package-private so are not importable from this package, and are therefore excluded from
 * {@code APPLICATION_TYPES}.
 */
class AuditArchitectureTests {

	private static final List<Class<?>> DOMAIN_TYPES = List.of(
			AuditEvent.class,
			AuditEventId.class
	);

	private static final List<Class<?>> APPLICATION_TYPES = List.of(
			RecordAuditEventUseCase.class,
			RecordAuditEventCommand.class,
			SearchAuditEventsUseCase.class,
			AuditEventSearchCriteria.class,
			AuditEventSearchResult.class,
			AuditEventView.class,
			AuditSearchValidationException.class
	);

	private static final List<Class<?>> PORT_TYPES = List.of(
			AuditRepositoryPort.class,
			AuditQueryRepositoryPort.class,
			AuditEventRow.class,
			AuditEventSearchQuery.class,
			AuditEventSearchPage.class
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

	@Test
	void portDoesNotReferenceApplication() {
		PORT_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(
				type, "za.co.tinyiko.transactionaggregation.audit.application"));
	}

	@Test
	void writeRepositoryPortExposesOnlySave() {
		List<String> methodNames = Arrays.stream(AuditRepositoryPort.class.getDeclaredMethods())
				.map(Method::getName)
				.toList();

		assertThat(methodNames).containsExactly("save");
	}

	/**
	 * The append-only guarantee for the read side holds structurally, not by convention: this
	 * port must never gain a method whose name implies a mutation (save/update/delete/insert/
	 * merge/persist), and today has exactly one method at all.
	 */
	@Test
	void queryRepositoryPortExposesNoMutatingMethods() {
		Method[] methods = AuditQueryRepositoryPort.class.getDeclaredMethods();

		assertThat(methods).hasSize(1);
		assertThat(methods[0].getName()).isEqualTo("search");
		assertThat(Arrays.stream(methods).map(Method::getName))
				.noneMatch(name -> {
					String lower = name.toLowerCase(Locale.ROOT);
					return lower.contains("save") || lower.contains("update") || lower.contains("delete")
							|| lower.contains("insert") || lower.contains("merge") || lower.contains("persist");
				});
	}

}
