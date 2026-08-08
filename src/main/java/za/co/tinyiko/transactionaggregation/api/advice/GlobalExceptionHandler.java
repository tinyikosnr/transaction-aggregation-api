package za.co.tinyiko.transactionaggregation.api.advice;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;

import za.co.tinyiko.transactionaggregation.categorisation.application.CategoryNotFoundException;
import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;
import za.co.tinyiko.transactionaggregation.transaction.application.DuplicateTransactionException;
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
 * {@code INVALID_DATE_RANGE}, {@code INTERNAL_SERVER_ERROR} - replacing the {@code TRX-001}-style
 * codes this class shipped with in {@code feature/api}, before the conflict was noticed. The two
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
 */
@RestControllerAdvice
class GlobalExceptionHandler {

	private static final String REQUEST_VALIDATION_FAILED_CODE = "REQUEST_VALIDATION_FAILED";

	@ExceptionHandler(DuplicateTransactionException.class)
	ResponseEntity<ProblemDetail> handleDuplicateTransaction(DuplicateTransactionException ex, HttpServletRequest request) {
		return respond(HttpStatus.CONFLICT, "TRANSACTION_DUPLICATE", ex.getMessage(), request);
	}

	@ExceptionHandler(TransactionSourceNotFoundException.class)
	ResponseEntity<ProblemDetail> handleTransactionSourceNotFound(TransactionSourceNotFoundException ex, HttpServletRequest request) {
		return respond(HttpStatus.NOT_FOUND, "SOURCE_NOT_FOUND", ex.getMessage(), request);
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

	@ExceptionHandler(TransactionValidationException.class)
	ResponseEntity<ProblemDetail> handleTransactionValidation(TransactionValidationException ex, HttpServletRequest request) {
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

	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
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
