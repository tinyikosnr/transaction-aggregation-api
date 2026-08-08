package za.co.tinyiko.transactionaggregation.api.advice;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
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
 * {@code api.controller} (TDS 39/40). Each mapped exception type stays in the module that
 * defines it; this class only translates a caught type to an HTTP status/{@code errorCode}, it
 * never constructs or throws one itself.
 *
 * <p>{@code TransactionValidationException} is deliberately mapped to a generic
 * {@code "VALIDATION_ERROR"} code, not one of TDS 40's specific {@code TRX-002}/{@code TRX-004}/
 * {@code TRX-005} codes - the exception itself does not currently carry which one applies (see
 * its own Javadoc and the plan's flagged gap), and string-matching its message to guess would be
 * fragile. Giving it a structured reason is deferred work, not silently resolved here.
 *
 * <p>{@code IllegalArgumentException} is handled generically (covering {@code DateRange}'s own
 * validation failures reaching this boundary via {@code AggregationApiMapper#toDateRange}) rather
 * than introducing a new named {@code aggregation.application.InvalidDateRangeException} for
 * this one call site - see the plan's reasoning for that choice.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

	private static final String VALIDATION_ERROR_CODE = "VALIDATION_ERROR";

	@ExceptionHandler(DuplicateTransactionException.class)
	ResponseEntity<ProblemDetail> handleDuplicateTransaction(DuplicateTransactionException ex, HttpServletRequest request) {
		return respond(HttpStatus.CONFLICT, "TRX-001", ex.getMessage(), request);
	}

	@ExceptionHandler(TransactionSourceNotFoundException.class)
	ResponseEntity<ProblemDetail> handleTransactionSourceNotFound(TransactionSourceNotFoundException ex, HttpServletRequest request) {
		return respond(HttpStatus.NOT_FOUND, "SRC-001", ex.getMessage(), request);
	}

	@ExceptionHandler({
			za.co.tinyiko.transactionaggregation.transaction.application.CustomerNotFoundException.class,
			za.co.tinyiko.transactionaggregation.aggregation.application.CustomerNotFoundException.class
	})
	ResponseEntity<ProblemDetail> handleCustomerNotFound(RuntimeException ex, HttpServletRequest request) {
		return respond(HttpStatus.NOT_FOUND, "CUS-001", ex.getMessage(), request);
	}

	@ExceptionHandler(CategoryNotFoundException.class)
	ResponseEntity<ProblemDetail> handleCategoryNotFound(CategoryNotFoundException ex, HttpServletRequest request) {
		return respond(HttpStatus.NOT_FOUND, "CAT-001", ex.getMessage(), request);
	}

	@ExceptionHandler(TransactionValidationException.class)
	ResponseEntity<ProblemDetail> handleTransactionValidation(TransactionValidationException ex, HttpServletRequest request) {
		return respond(HttpStatus.BAD_REQUEST, VALIDATION_ERROR_CODE, ex.getMessage(), request);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
		return respond(HttpStatus.BAD_REQUEST, "AGG-001", ex.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ProblemDetail> handleBeanValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		String detail = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return respond(HttpStatus.BAD_REQUEST, VALIDATION_ERROR_CODE, detail, request);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
		String requiredType = ex.getRequiredType() == null ? "a valid value" : ex.getRequiredType().getSimpleName();
		return respond(HttpStatus.BAD_REQUEST, VALIDATION_ERROR_CODE,
				"%s must be %s".formatted(ex.getName(), requiredType), request);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	ResponseEntity<ProblemDetail> handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
		return respond(HttpStatus.BAD_REQUEST, VALIDATION_ERROR_CODE, ex.getMessage(), request);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
		return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", request);
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
