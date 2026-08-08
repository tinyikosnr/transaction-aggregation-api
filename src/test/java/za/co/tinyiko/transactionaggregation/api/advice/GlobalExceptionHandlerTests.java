package za.co.tinyiko.transactionaggregation.api.advice;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MissingServletRequestParameterException;

import za.co.tinyiko.transactionaggregation.categorisation.application.CategoryNotFoundException;
import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;
import za.co.tinyiko.transactionaggregation.transaction.application.CustomerNotFoundException;
import za.co.tinyiko.transactionaggregation.transaction.application.DuplicateTransactionException;
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
		assertThat(response.getBody().getProperties()).containsEntry("errorCode", "TRX-001");
		assertThat(response.getBody().getProperties()).containsEntry("correlationId", "handler-test-id");
	}

	@Test
	void mapsTransactionSourceNotFoundToNotFoundWithSrc001() {
		ResponseEntity<ProblemDetail> response = handler.handleTransactionSourceNotFound(
				new TransactionSourceNotFoundException("MOCK_BANK_A"), requestWithCorrelationId());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody().getProperties()).containsEntry("errorCode", "SRC-001");
	}

	@Test
	void mapsTransactionCustomerNotFoundToNotFoundWithCus001() {
		ResponseEntity<ProblemDetail> response = handler.handleCustomerNotFound(
				new CustomerNotFoundException(UUID.randomUUID()), requestWithCorrelationId());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody().getProperties()).containsEntry("errorCode", "CUS-001");
	}

	@Test
	void mapsAggregationCustomerNotFoundToNotFoundWithCus001() {
		ResponseEntity<ProblemDetail> response = handler.handleCustomerNotFound(
				new za.co.tinyiko.transactionaggregation.aggregation.application.CustomerNotFoundException(UUID.randomUUID()),
				requestWithCorrelationId());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody().getProperties()).containsEntry("errorCode", "CUS-001");
	}

	@Test
	void mapsCategoryNotFoundToNotFoundWithCat001() {
		ResponseEntity<ProblemDetail> response = handler.handleCategoryNotFound(
				new CategoryNotFoundException(UUID.randomUUID()), requestWithCorrelationId());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody().getProperties()).containsEntry("errorCode", "CAT-001");
	}

	@Test
	void mapsTransactionValidationToBadRequestWithGenericValidationCode() {
		ResponseEntity<ProblemDetail> response = handler.handleTransactionValidation(
				new TransactionValidationException("amount must be greater than zero", null), requestWithCorrelationId());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody().getProperties()).containsEntry("errorCode", "VALIDATION_ERROR");
	}

	@Test
	void mapsIllegalArgumentToBadRequestWithAgg001() {
		ResponseEntity<ProblemDetail> response = handler.handleIllegalArgument(
				new IllegalArgumentException("date range must not exceed 24 months"), requestWithCorrelationId());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody().getProperties()).containsEntry("errorCode", "AGG-001");
	}

	@Test
	void mapsMissingParameterToBadRequestWithGenericValidationCode() {
		ResponseEntity<ProblemDetail> response = handler.handleMissingParameter(
				new MissingServletRequestParameterException("from", "LocalDate"), requestWithCorrelationId());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody().getProperties()).containsEntry("errorCode", "VALIDATION_ERROR");
	}

	@Test
	void mapsUnexpectedExceptionToInternalServerErrorWithoutLeakingDetails() {
		ResponseEntity<ProblemDetail> response = handler.handleUnexpected(
				new RuntimeException("some internal implementation detail"), requestWithCorrelationId());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody().getProperties()).containsEntry("errorCode", "INTERNAL_ERROR");
		assertThat(response.getBody().getDetail()).doesNotContain("some internal implementation detail");
	}

	@Test
	void omitsCorrelationIdPropertyWhenRequestHasNoAttribute() {
		ResponseEntity<ProblemDetail> response = handler.handleCategoryNotFound(
				new CategoryNotFoundException(UUID.randomUUID()), new MockHttpServletRequest());

		assertThat(response.getBody().getProperties()).doesNotContainKey("correlationId");
	}

}
