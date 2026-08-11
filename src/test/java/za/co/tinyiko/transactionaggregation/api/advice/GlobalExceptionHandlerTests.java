package za.co.tinyiko.transactionaggregation.api.advice;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MissingServletRequestParameterException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import za.co.tinyiko.transactionaggregation.categorisation.application.CategoryNotFoundException;
import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;
import za.co.tinyiko.transactionaggregation.transaction.application.CustomerNotFoundException;
import za.co.tinyiko.transactionaggregation.transaction.application.DuplicateTransactionException;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionNotFoundException;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionSourceNotFoundException;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionValidationException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Direct unit tests against {@link GlobalExceptionHandler}'s own methods - no Spring context,
 * matching this codebase's usual preference to reserve slice/integration tests for what actually
 * needs a container. HTTP-level wiring (that Spring actually routes each exception type here) is
 * covered separately by {@code TransactionControllerTests}/{@code AggregationControllerTests}.
 */
class GlobalExceptionHandlerTests {

	private static final CorrelationId CORRELATION_ID = new CorrelationId("handler-test-id");

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	private static MockHttpServletRequest requestWithCorrelationId() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(CorrelationId.REQUEST_ATTRIBUTE_NAME, CORRELATION_ID);
		return request;
	}

	@Test
	void mapsDuplicateTransactionToConflictWithTrx001() {
		ResponseEntity<ProblemDetail> response = handler.handleDuplicateTransaction(
				new DuplicateTransactionException(UUID.randomUUID(), "EXT-001"), requestWithCorrelationId());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody().getProperties()).containsEntry("errorCode", "TRANSACTION_DUPLICATE");
		assertThat(response.getBody().getProperties()).containsEntry("correlationId", "handler-test-id");
	}

	@Test
	void mapsTransactionSourceNotFoundToNotFoundWithSrc001() {
		ResponseEntity<ProblemDetail> response = handler.handleTransactionSourceNotFound(
				new TransactionSourceNotFoundException("MOCK_BANK_A"), requestWithCorrelationId());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody().getProperties()).containsEntry("errorCode", "SOURCE_NOT_FOUND");
	}

	@Test
	void mapsTransactionNotFoundToNotFoundWithTransactionNotFoundCode() {
		ResponseEntity<ProblemDetail> response = handler.handleTransactionNotFound(
				new TransactionNotFoundException(UUID.randomUUID()), requestWithCorrelationId());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody().getProperties()).containsEntry("errorCode", "TRANSACTION_NOT_FOUND");
	}

	@Test
	void mapsTransactionCustomerNotFoundToNotFoundWithCus001() {
		ResponseEntity<ProblemDetail> response = handler.handleCustomerNotFound(
				new CustomerNotFoundException(UUID.randomUUID()), requestWithCorrelationId());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody().getProperties()).containsEntry("errorCode", "CUSTOMER_NOT_FOUND");
	}

	@Test
	void mapsAggregationCustomerNotFoundToNotFoundWithCus001() {
		ResponseEntity<ProblemDetail> response = handler.handleCustomerNotFound(
				new za.co.tinyiko.transactionaggregation.aggregation.application.CustomerNotFoundException(UUID.randomUUID()),
				requestWithCorrelationId());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody().getProperties()).containsEntry("errorCode", "CUSTOMER_NOT_FOUND");
	}

	@Test
	void mapsCategoryNotFoundToNotFoundWithCat001() {
		ResponseEntity<ProblemDetail> response = handler.handleCategoryNotFound(
				new CategoryNotFoundException(UUID.randomUUID()), requestWithCorrelationId());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody().getProperties()).containsEntry("errorCode", "CATEGORY_NOT_FOUND");
	}

	@Test
	void mapsTransactionValidationToBadRequestWithGenericValidationCode() {
		ResponseEntity<ProblemDetail> response = handler.handleTransactionValidation(
				new TransactionValidationException("amount must be greater than zero", null), requestWithCorrelationId());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody().getProperties()).containsEntry("errorCode", "REQUEST_VALIDATION_FAILED");
	}

	@Test
	void mapsIllegalArgumentToBadRequestWithAgg001() {
		ResponseEntity<ProblemDetail> response = handler.handleIllegalArgument(
				new IllegalArgumentException("date range must not exceed 24 months"), requestWithCorrelationId());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody().getProperties()).containsEntry("errorCode", "INVALID_DATE_RANGE");
	}

	@Test
	void mapsMissingParameterToBadRequestWithGenericValidationCode() {
		ResponseEntity<ProblemDetail> response = handler.handleMissingParameter(
				new MissingServletRequestParameterException("from", "LocalDate"), requestWithCorrelationId());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody().getProperties()).containsEntry("errorCode", "REQUEST_VALIDATION_FAILED");
	}

	@Test
	void mapsUnexpectedExceptionToInternalServerErrorWithoutLeakingDetails() {
		ResponseEntity<ProblemDetail> response = handler.handleUnexpected(
				new RuntimeException("some internal implementation detail"), requestWithCorrelationId());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody().getProperties()).containsEntry("errorCode", "INTERNAL_SERVER_ERROR");
		assertThat(response.getBody().getDetail()).doesNotContain("some internal implementation detail");
	}

	@Test
	void omitsCorrelationIdPropertyWhenRequestHasNoAttribute() {
		ResponseEntity<ProblemDetail> response = handler.handleCategoryNotFound(
				new CategoryNotFoundException(UUID.randomUUID()), new MockHttpServletRequest());

		assertThat(response.getBody().getProperties()).doesNotContainKey("correlationId");
	}

	/**
	 * SAD 39: "Unexpected exceptions are logged once at the boundary." Asserts exactly one
	 * {@code ERROR} event, carrying the real {@link Throwable} (not a hand-built string) so the
	 * structured JSON formatter can serialize {@code error.type}/{@code error.message}/
	 * {@code error.stack_trace} natively.
	 */
	@Test
	void logsExactlyOneErrorEventWithTheThrowableAttachedForAnUnexpectedException() {
		ListAppender<ILoggingEvent> appender = attachTestAppender();
		try {
			RuntimeException thrown = new RuntimeException("some internal implementation detail");

			handler.handleUnexpected(thrown, requestWithCorrelationId());

			assertThat(appender.list).hasSize(1);
			ILoggingEvent event = appender.list.get(0);
			assertThat(event.getLevel()).isEqualTo(Level.ERROR);
			assertThat(event.getThrowableProxy().getClassName()).isEqualTo(RuntimeException.class.getName());
			assertThat(event.getThrowableProxy().getMessage()).isEqualTo("some internal implementation detail");
		} finally {
			detachTestAppender(appender);
		}
	}

	/**
	 * Expected business/validation failures (4xx) already have full traceability through their
	 * {@code errorCode}/{@code correlationId} and, where applicable, the audit trail - logging them
	 * again here would duplicate that trail and spam {@code ERROR}-level noise for ordinary,
	 * client-driven outcomes.
	 */
	@Test
	void doesNotLogAnythingForAnExpectedBusinessException() {
		ListAppender<ILoggingEvent> appender = attachTestAppender();
		try {
			handler.handleDuplicateTransaction(
					new DuplicateTransactionException(UUID.randomUUID(), "EXT-001"), requestWithCorrelationId());

			assertThat(appender.list).isEmpty();
		} finally {
			detachTestAppender(appender);
		}
	}

	private static ListAppender<ILoggingEvent> attachTestAppender() {
		Logger logbackLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logbackLogger.addAppender(appender);
		return appender;
	}

	private static void detachTestAppender(ListAppender<ILoggingEvent> appender) {
		Logger logbackLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
		logbackLogger.detachAppender(appender);
		appender.stop();
	}

}
