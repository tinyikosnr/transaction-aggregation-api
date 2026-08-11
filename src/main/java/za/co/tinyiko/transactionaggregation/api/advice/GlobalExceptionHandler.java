package za.co.tinyiko.transactionaggregation.api.advice;

import java.time.Instant;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

import za.co.tinyiko.transactionaggregation.audit.application.AuditSearchValidationException;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategoryNotFoundException;
import za.co.tinyiko.transactionaggregation.categorisation.application.RuleConflictException;
import za.co.tinyiko.transactionaggregation.categorisation.application.RuleNotFoundException;
import za.co.tinyiko.transactionaggregation.categorisation.application.RuleValidationException;
import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;
import za.co.tinyiko.transactionaggregation.transaction.application.DuplicateTransactionException;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionNotFoundException;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionSourceNotFoundException;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionValidationException;

/**
 * Central RFC 9457 ({@link ProblemDetail}) exception translation for every controller in
 * {@code api.controller} (SAD 39). Each mapped exception type stays in the module that defines
 * it; this class only translates a caught type to an HTTP status/{@code errorCode}, it never
 * constructs or throws one itself.
 *
 * <p>Error codes match SAD 39.4 exactly (not TDS 40's {@code TRX-NNN}-style catalogue) - a
 * genuine SAD/TDS naming conflict, resolved per documentation precedence (SAD outranks TDS) in
 * {@code feature/security}: {@code TRANSACTION_DUPLICATE}, {@code SOURCE_NOT_FOUND},
 * {@code CUSTOMER_NOT_FOUND}, {@code CATEGORY_NOT_FOUND}, {@code REQUEST_VALIDATION_FAILED},
 * {@code INVALID_DATE_RANGE}, {@code INTERNAL_SERVER_ERROR}, {@code TRANSACTION_NOT_FOUND}
 * (added in {@code feature/transaction-query}, already present in SAD 39.4's catalogue but unused
 * until now), {@code OPTIMISTIC_LOCK_CONFLICT} (added in {@code feature/category-admin}, also
 * already present in SAD 39.4 but unused until now) - replacing the {@code TRX-001}-style
 * codes this class shipped with in {@code feature/api}, before the conflict was noticed.
 * {@code RULE_NOT_FOUND} (also added in {@code feature/category-admin}) is a genuinely new code,
 * not part of SAD 39.4 - an explicit project decision, since no rule-specific not-found code is
 * documented anywhere. {@code feature/audit-query} adds no new code at all -
 * {@code AuditSearchValidationException} reuses the existing {@code REQUEST_VALIDATION_FAILED}.
 * The two
 * 401 codes ({@code AUTHENTICATION_REQUIRED}, {@code TOKEN_INVALID}) and the 403 code
 * ({@code ACCESS_DENIED}) never reach this class at all - see
 * {@code security.ProblemDetailAuthenticationEntryPoint}/{@code ProblemDetailAccessDeniedHandler}.
 *
 * <p>{@code TransactionValidationException} is deliberately mapped to a generic
 * {@code "REQUEST_VALIDATION_FAILED"} code, not one of TDS 40's specific {@code TRX-002}/
 * {@code TRX-004}/{@code TRX-005} codes - the exception itself does not currently carry which one
 * applies (see its own Javadoc and the plan's flagged gap), and string-matching its message to
 * guess would be fragile. Giving it a structured reason is deferred work, not silently resolved
 * here.
 *
 * <p>{@code IllegalArgumentException} is handled generically (covering {@code DateRange}'s own
 * validation failures reaching this boundary via {@code aggregation.application}) rather than
 * introducing a new named {@code aggregation.application.InvalidDateRangeException} for this one
 * call site - see the plan's reasoning for that choice.
 *
 * <p>{@link AccessDeniedException} (and its subtype {@code AuthorizationDeniedException}, thrown
 * by {@code @PreAuthorize}'s method-security interceptor around a controller method call) is
 * deliberately rethrown, not handled - found empirically, not assumed: without this, this class's
 * own {@code @ExceptionHandler(Exception.class)} catch-all matched it first (Spring MVC's
 * exception resolution runs inside {@code DispatcherServlet}'s dispatch, before the exception
 * would otherwise reach Spring Security's {@code ExceptionTranslationFilter}), turning every 403
 * into a 500 and never letting {@code security.ProblemDetailAccessDeniedHandler} run. Rethrowing
 * from an {@code @ExceptionHandler} method is the standard way to make
 * {@code ExceptionHandlerExceptionResolver} treat it as unresolved, so {@code DispatcherServlet}
 * propagates it back out to the servlet filter chain instead.
 *
 * <p><strong>Exception logging (feature/structured-logging, SAD 39: "Unexpected exceptions are
 * logged once at the boundary"):</strong> only {@link #handleUnexpected} logs anything, at
 * {@code ERROR}, passing the real {@link Exception} to SLF4J rather than hand-building a message -
 * Spring Boot's structured JSON formatter serializes it into {@code error.type}/
 * {@code error.message}/{@code error.stack_trace} natively. Every other handler in this class
 * (expected business/validation failures - duplicates, not-found, validation, conflict) logs
 * nothing: each already has full traceability through its {@code errorCode}/{@code correlationId}
 * in the response and, for transaction failures, through the audit trail
 * ({@code CreateTransactionService.recordFailure}) - logging them again here would duplicate that
 * trail and spam low-value {@code ERROR} noise for ordinary client-driven outcomes.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	private static final String REQUEST_VALIDATION_FAILED_CODE = "REQUEST_VALIDATION_FAILED";

	@ExceptionHandler(DuplicateTransactionException.class)
	ResponseEntity<ProblemDetail> handleDuplicateTransaction(DuplicateTransactionException ex, HttpServletRequest request) {
		return respond(HttpStatus.CONFLICT, "TRANSACTION_DUPLICATE", ex.getMessage(), request);
	}

	@ExceptionHandler(TransactionSourceNotFoundException.class)
	ResponseEntity<ProblemDetail> handleTransactionSourceNotFound(TransactionSourceNotFoundException ex, HttpServletRequest request) {
		return respond(HttpStatus.NOT_FOUND, "SOURCE_NOT_FOUND", ex.getMessage(), request);
	}

	@ExceptionHandler(TransactionNotFoundException.class)
	ResponseEntity<ProblemDetail> handleTransactionNotFound(TransactionNotFoundException ex, HttpServletRequest request) {
		return respond(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", ex.getMessage(), request);
	}

	@ExceptionHandler({
			za.co.tinyiko.transactionaggregation.transaction.application.CustomerNotFoundException.class,
			za.co.tinyiko.transactionaggregation.aggregation.application.CustomerNotFoundException.class
	})
	ResponseEntity<ProblemDetail> handleCustomerNotFound(RuntimeException ex, HttpServletRequest request) {
		return respond(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", ex.getMessage(), request);
	}

	@ExceptionHandler(CategoryNotFoundException.class)
	ResponseEntity<ProblemDetail> handleCategoryNotFound(CategoryNotFoundException ex, HttpServletRequest request) {
		return respond(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", ex.getMessage(), request);
	}

	@ExceptionHandler(RuleNotFoundException.class)
	ResponseEntity<ProblemDetail> handleRuleNotFound(RuleNotFoundException ex, HttpServletRequest request) {
		return respond(HttpStatus.NOT_FOUND, "RULE_NOT_FOUND", ex.getMessage(), request);
	}

	@ExceptionHandler(RuleConflictException.class)
	ResponseEntity<ProblemDetail> handleRuleConflict(RuleConflictException ex, HttpServletRequest request) {
		return respond(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK_CONFLICT", ex.getMessage(), request);
	}

	@ExceptionHandler(RuleValidationException.class)
	ResponseEntity<ProblemDetail> handleRuleValidation(RuleValidationException ex, HttpServletRequest request) {
		return respond(HttpStatus.BAD_REQUEST, REQUEST_VALIDATION_FAILED_CODE, ex.getMessage(), request);
	}

	@ExceptionHandler(TransactionValidationException.class)
	ResponseEntity<ProblemDetail> handleTransactionValidation(TransactionValidationException ex, HttpServletRequest request) {
		return respond(HttpStatus.BAD_REQUEST, REQUEST_VALIDATION_FAILED_CODE, ex.getMessage(), request);
	}

	@ExceptionHandler(AuditSearchValidationException.class)
	ResponseEntity<ProblemDetail> handleAuditSearchValidation(AuditSearchValidationException ex, HttpServletRequest request) {
		return respond(HttpStatus.BAD_REQUEST, REQUEST_VALIDATION_FAILED_CODE, ex.getMessage(), request);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
		return respond(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", ex.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ProblemDetail> handleBeanValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		String detail = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return respond(HttpStatus.BAD_REQUEST, REQUEST_VALIDATION_FAILED_CODE, detail, request);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
		String requiredType = ex.getRequiredType() == null ? "a valid value" : ex.getRequiredType().getSimpleName();
		return respond(HttpStatus.BAD_REQUEST, REQUEST_VALIDATION_FAILED_CODE,
				"%s must be %s".formatted(ex.getName(), requiredType), request);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	ResponseEntity<ProblemDetail> handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
		return respond(HttpStatus.BAD_REQUEST, REQUEST_VALIDATION_FAILED_CODE, ex.getMessage(), request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	void rethrowAccessDenied(AccessDeniedException ex) {
		throw ex;
	}

	/**
	 * Same rethrow technique as {@link #rethrowAccessDenied}, for the same reason: without this,
	 * the catch-all {@code @ExceptionHandler(Exception.class)} below matched
	 * {@link NoResourceFoundException} first, turning every request against an unmapped path into
	 * a 500 instead of a 404 - invisible until {@code feature/openapi-documentation} added
	 * {@code permitAll()} matchers for the (by-default-disabled) springdoc paths, since every other
	 * unmapped path in this API sits behind {@code anyRequest().authenticated()} and gets a 401
	 * from Spring Security before ever reaching {@code DispatcherServlet}'s handler resolution.
	 * {@link NoResourceFoundException} already extends {@code ErrorResponseException} and carries
	 * its own correct 404 {@link ProblemDetail}; rethrowing lets Spring's own default
	 * {@code HandlerExceptionResolver} chain render it, rather than reinventing an equivalent
	 * {@code ProblemDetail} here.
	 */
	@ExceptionHandler(NoResourceFoundException.class)
	void rethrowNoResourceFound(NoResourceFoundException ex) throws NoResourceFoundException {
		throw ex;
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unexpected exception handling request", ex);
		return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected error occurred", request);
	}

	private ResponseEntity<ProblemDetail> respond(HttpStatus status, String errorCode, String detail, HttpServletRequest request) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
		problemDetail.setProperty("errorCode", errorCode);
		problemDetail.setProperty("timestamp", Instant.now());
		if (request.getAttribute(CorrelationId.REQUEST_ATTRIBUTE_NAME) instanceof CorrelationId correlationId) {
			problemDetail.setProperty("correlationId", correlationId.value());
		}
		return ResponseEntity.status(status).body(problemDetail);
	}

}
