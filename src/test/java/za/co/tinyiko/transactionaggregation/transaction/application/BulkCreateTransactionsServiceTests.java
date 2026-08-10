package za.co.tinyiko.transactionaggregation.transaction.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkCreateTransactionsServiceTests {

	private static final CorrelationId CORRELATION_ID = new CorrelationId("bulk-correlation-id");
	private static final String ACTOR = "jwt-subject-001";
	private static final UUID CUSTOMER_ID = UUID.randomUUID();

	@Mock
	private CreateTransactionUseCase createTransactionUseCase;

	private BulkCreateTransactionsService service() {
		return new BulkCreateTransactionsService(createTransactionUseCase);
	}

	private static BulkTransactionItemInput anItem(String externalId) {
		return new BulkTransactionItemInput(externalId, CUSTOMER_ID, "MOCK_BANK_A",
				new BigDecimal("10.00"), "ZAR", "DEBIT", "desc", "Checkers", Instant.parse("2026-08-06T08:00:00Z"));
	}

	private static TransactionCreatedResult aResult(UUID id) {
		return new TransactionCreatedResult(id, CUSTOMER_ID, "MOCK_BANK_A", "EXT-001",
				UUID.randomUUID(), "Checkers", "GROCERIES", "Groceries", new BigDecimal("10.00"), "ZAR", "DEBIT",
				"desc", "PROCESSED", Instant.parse("2026-08-06T08:00:00Z"),
				Instant.parse("2026-08-06T08:00:01Z"), Instant.parse("2026-08-06T08:00:01Z"));
	}

	@Test
	void allItemsCreatedProducesAFullySuccessfulResultInOrder() {
		UUID id1 = UUID.randomUUID();
		UUID id2 = UUID.randomUUID();
		when(createTransactionUseCase.create(any())).thenReturn(aResult(id1), aResult(id2));

		BulkCreateTransactionsCommand command = new BulkCreateTransactionsCommand(
				List.of(anItem("EXT-001"), anItem("EXT-002")), CORRELATION_ID, ACTOR);

		BulkTransactionResult result = service().create(command);

		assertThat(result.total()).isEqualTo(2);
		assertThat(result.successful()).isEqualTo(2);
		assertThat(result.failed()).isZero();
		assertThat(result.results()).extracting(BulkTransactionItemResult::created).containsExactly(true, true);
	}

	@Test
	void mixedOutcomesArePreservedInOriginalOrderWithCorrectTotals() {
		when(createTransactionUseCase.create(any()))
				.thenReturn(aResult(UUID.randomUUID()))
				.thenThrow(new DuplicateTransactionException(UUID.randomUUID(), "EXT-002"))
				.thenThrow(new CustomerNotFoundException(CUSTOMER_ID))
				.thenThrow(new TransactionSourceNotFoundException("MOCK_BANK_A"))
				.thenThrow(new TransactionValidationException("amount must be greater than zero", null));

		BulkCreateTransactionsCommand command = new BulkCreateTransactionsCommand(
				List.of(anItem("EXT-001"), anItem("EXT-002"), anItem("EXT-003"), anItem("EXT-004"), anItem("EXT-005")),
				CORRELATION_ID, ACTOR);

		BulkTransactionResult result = service().create(command);

		assertThat(result.total()).isEqualTo(5);
		assertThat(result.successful()).isEqualTo(1);
		assertThat(result.failed()).isEqualTo(4);
		assertThat(result.results()).extracting(BulkTransactionItemResult::created)
				.containsExactly(true, false, false, false, false);
		assertThat(result.results()).extracting(BulkTransactionItemResult::errorCode)
				.containsExactly(null, "TRANSACTION_DUPLICATE", "CUSTOMER_NOT_FOUND", "SOURCE_NOT_FOUND", "REQUEST_VALIDATION_FAILED");
	}

	@Test
	void missingRequiredFieldFailsThatItemAsRequestValidationFailedWithoutCallingTheUseCase() {
		BulkTransactionItemInput missingCustomerId = new BulkTransactionItemInput(
				"EXT-001", null, "MOCK_BANK_A", new BigDecimal("10.00"), "ZAR", "DEBIT", "desc", "Checkers",
				Instant.parse("2026-08-06T08:00:00Z"));
		BulkCreateTransactionsCommand command = new BulkCreateTransactionsCommand(
				List.of(missingCustomerId), CORRELATION_ID, ACTOR);

		BulkTransactionResult result = service().create(command);

		assertThat(result.results()).hasSize(1);
		assertThat(result.results().get(0).created()).isFalse();
		assertThat(result.results().get(0).errorCode()).isEqualTo("REQUEST_VALIDATION_FAILED");
		verify(createTransactionUseCase, never()).create(any());
	}

	@Test
	void anUnexpectedRuntimeExceptionIsNotCaughtAndPropagatesOutOfCreate() {
		when(createTransactionUseCase.create(any()))
				.thenReturn(aResult(UUID.randomUUID()))
				.thenThrow(new IllegalStateException("database connection lost"));

		BulkCreateTransactionsCommand command = new BulkCreateTransactionsCommand(
				List.of(anItem("EXT-001"), anItem("EXT-002"), anItem("EXT-003")), CORRELATION_ID, ACTOR);

		assertThatThrownBy(() -> service().create(command)).isInstanceOf(IllegalStateException.class);
		verify(createTransactionUseCase, times(2)).create(any());
	}

	@Test
	void everyItemSharesTheSameActorAndCorrelationIdFromTheBatchCommand() {
		when(createTransactionUseCase.create(any())).thenReturn(aResult(UUID.randomUUID()));
		BulkCreateTransactionsCommand command = new BulkCreateTransactionsCommand(
				List.of(anItem("EXT-001"), anItem("EXT-002")), CORRELATION_ID, ACTOR);

		service().create(command);

		ArgumentCaptor<CreateTransactionCommand> captor = ArgumentCaptor.forClass(CreateTransactionCommand.class);
		verify(createTransactionUseCase, times(2)).create(captor.capture());
		assertThat(captor.getAllValues()).allSatisfy(c -> {
			assertThat(c.actor()).isEqualTo(ACTOR);
			assertThat(c.correlationId()).isEqualTo(CORRELATION_ID);
		});
	}

	@Test
	void anEmptyItemListProducesAnEmptyAllZeroResult() {
		BulkCreateTransactionsCommand command = new BulkCreateTransactionsCommand(List.of(), CORRELATION_ID, ACTOR);

		BulkTransactionResult result = service().create(command);

		assertThat(result.total()).isZero();
		assertThat(result.successful()).isZero();
		assertThat(result.failed()).isZero();
		assertThat(result.results()).isEmpty();
		verify(createTransactionUseCase, never()).create(any());
	}

}
