/**
 * Shared presentation layer for all REST APIs: controllers, request/response DTOs and
 * global exception handling.
 *
 * <p>This package contains no business logic. It may depend only on the {@code application}
 * package of each business module — their use-case interfaces — and must never depend on a
 * module's {@code domain} or {@code persistence} packages directly. A JPA or domain entity
 * must never be returned to a client; always map to a purpose-specific response DTO.
 *
 * <p>{@code allowedDependencies} lists only what this branch's controllers,
 * {@code api.mapper.TransactionApiMapper} and {@code api.advice.GlobalExceptionHandler} genuinely
 * reference: {@code transaction} for {@code POST /api/v1/transactions}, {@code aggregation} for
 * the four customer summary endpoints, {@code categorisation} solely to catch
 * {@code CategoryNotFoundException} in the global exception handler (thrown by the category
 * lookup {@code CreateTransactionService} makes internally - {@code api} never calls
 * {@code categorisation.application} directly itself), {@code audit} for
 * {@code GET /api/v1/audit-events} ({@code feature/audit-query} - the first time {@code api}
 * calls {@code audit.application} directly, via {@code AuditEventController}, not just to catch
 * an exception thrown internally by another module), and {@code shared} for {@code CorrelationId}
 * (the create-transaction mapper builds one into the command; the exception handler reads one
 * back off the request attribute). No {@code merchant} dependency - merchant enrichment for the
 * create-transaction response is composed entirely inside {@code transaction.application}. Each
 * business-module entry uses the qualified {@code "module :: application"} syntax - see
 * {@code transaction}'s own package-info for why a bare module name isn't equivalent;
 * {@code shared} stays a bare name since it is {@code type = OPEN} and therefore accessible
 * regardless.
 */
@org.springframework.modulith.ApplicationModule(
		allowedDependencies = {
				"transaction :: application",
				"aggregation :: application",
				"categorisation :: application",
				"audit :: application",
				"shared"
		})
package za.co.tinyiko.transactionaggregation.api;
