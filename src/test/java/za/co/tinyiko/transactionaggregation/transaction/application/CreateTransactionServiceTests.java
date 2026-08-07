package za.co.tinyiko.transactionaggregation.transaction.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.ObjectMapper;

import za.co.tinyiko.transactionaggregation.audit.application.RecordAuditEventCommand;
import za.co.tinyiko.transactionaggregation.audit.application.RecordAuditEventUseCase;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategorisationDecision;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategorisationInput;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategoriseTransactionUseCase;
import za.co.tinyiko.transactionaggregation.customer.application.CustomerExistsPort;
import za.co.tinyiko.transactionaggregation.merchant.application.MerchantResolutionPort;
import za.co.tinyiko.transactionaggregation.merchant.application.MerchantResolutionResult;
import za.co.tinyiko.transactionaggregation.transaction.domain.Money;
import za.co.tinyiko.transactionaggregation.transaction.domain.SourceStatus;
import za.co.tinyiko.transactionaggregation.transaction.domain.Transaction;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionDirection;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionId;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionSource;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionSourceId;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionRepositoryPort;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSourceRepositoryPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTransactionServiceTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-06T08:15:00Z"), ZoneOffset.UTC);
	private static final UUID CUSTOMER_ID = UUID.randomUUID();
	private static final UUID CATEGORY_ID = UUID.randomUUID();
	private static final UUID MERCHANT_ID = UUID.randomUUID();

	@Mock
	private TransactionRepositoryPort transactionRepositoryPort;
	@Mock
	private TransactionSourceRepositoryPort transactionSourceRepositoryPort;
	@Mock
	private CustomerExistsPort customerExistsPort;
	@Mock
	private MerchantResolutionPort merchantResolutionPort;
	@Mock
	private CategoriseTransactionUseCase categoriseTransactionUseCase;
	@Mock
	private RecordAuditEventUseCase recordAuditEventUseCase;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private CreateTransactionService service() {
		return new CreateTransactionService(transactionRepositoryPort, transactionSourceRepositoryPort,
				customerExistsPort, merchantResolutionPort, categoriseTransactionUseCase, recordAuditEventUseCase,
				FIXED_CLOCK, objectMapper);
	}

	private static TransactionSource activeSource() {
		return TransactionSource.register(TransactionSourceId.generate(), "MOCK_BANK_A", "Mock Bank A",
				SourceStatus.ACTIVE, FIXED_CLOCK);
	}

	private static CreateTransactionCommand aCommand() {
		return new CreateTransactionCommand("EXT-001", CUSTOMER_ID, "MOCK_BANK_A", new BigDecimal("125.50"), "ZAR",
				"DEBIT", "Checkers Centurion", "Checkers", Instant.parse("2026-08-06T08:00:00Z"));
	}

	@Test
	void createsAndPersistsATransactionOnTheHappyPath() {
		TransactionSource source = activeSource();
		when(transactionSourceRepositoryPort.findByCode("MOCK_BANK_A")).thenReturn(Optional.of(source));
		when(customerExistsPort.exists(CUSTOMER_ID)).thenReturn(true);
		when(transactionRepositoryPort.findBySourceIdAndExternalTransactionId(source.id(), "EXT-001"))
				.thenReturn(Optional.empty());
		when(merchantResolutionPort.resolve("Checkers")).thenReturn(new MerchantResolutionResult(MERCHANT_ID, "Checkers"));
		when(categoriseTransactionUseCase.categorise(any(CategorisationInput.class)))
				.thenReturn(new CategorisationDecision(CATEGORY_ID, UUID.randomUUID(), "Merchant contained CHECKERS"));
		when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Transaction result = service().create(aCommand());

		assertThat(result.customerId()).isEqualTo(CUSTOMER_ID);
		assertThat(result.categoryId()).isEqualTo(CATEGORY_ID);
		assertThat(result.merchantId()).isEqualTo(MERCHANT_ID);

		ArgumentCaptor<RecordAuditEventCommand> auditCaptor = ArgumentCaptor.forClass(RecordAuditEventCommand.class);
		verify(recordAuditEventUseCase).record(auditCaptor.capture());
		assertThat(auditCaptor.getValue().eventType()).isEqualTo("TRANSACTION_CREATED");
	}

	@Test
	void requestsCategorisationWithTheMerchantDisplayNameNotNormalisedName() {
		TransactionSource source = activeSource();
		when(transactionSourceRepositoryPort.findByCode("MOCK_BANK_A")).thenReturn(Optional.of(source));
		when(customerExistsPort.exists(CUSTOMER_ID)).thenReturn(true);
		when(transactionRepositoryPort.findBySourceIdAndExternalTransactionId(source.id(), "EXT-001"))
				.thenReturn(Optional.empty());
		when(merchantResolutionPort.resolve("Dis-Chem Pharmacy"))
				.thenReturn(new MerchantResolutionResult(MERCHANT_ID, "Dis-Chem Pharmacy"));
		when(categoriseTransactionUseCase.categorise(any(CategorisationInput.class)))
				.thenReturn(new CategorisationDecision(CATEGORY_ID, null, "fallback"));
		when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CreateTransactionCommand command = new CreateTransactionCommand("EXT-001", CUSTOMER_ID, "MOCK_BANK_A",
				new BigDecimal("125.50"), "ZAR", "DEBIT", "pharmacy purchase", "Dis-Chem Pharmacy",
				Instant.parse("2026-08-06T08:00:00Z"));

		service().create(command);

		ArgumentCaptor<CategorisationInput> captor = ArgumentCaptor.forClass(CategorisationInput.class);
		verify(categoriseTransactionUseCase).categorise(captor.capture());
		assertThat(captor.getValue().merchantText()).isEqualTo("Dis-Chem Pharmacy");
	}

	@Test
	void doesNotResolveMerchantWhenMerchantNameIsBlank() {
		TransactionSource source = activeSource();
		when(transactionSourceRepositoryPort.findByCode("MOCK_BANK_A")).thenReturn(Optional.of(source));
		when(customerExistsPort.exists(CUSTOMER_ID)).thenReturn(true);
		when(transactionRepositoryPort.findBySourceIdAndExternalTransactionId(source.id(), "EXT-001"))
				.thenReturn(Optional.empty());
		when(categoriseTransactionUseCase.categorise(any(CategorisationInput.class)))
				.thenReturn(new CategorisationDecision(CATEGORY_ID, null, "fallback"));
		when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CreateTransactionCommand command = new CreateTransactionCommand("EXT-001", CUSTOMER_ID, "MOCK_BANK_A",
				new BigDecimal("125.50"), "ZAR", "DEBIT", "unknown", " ", Instant.parse("2026-08-06T08:00:00Z"));

		Transaction result = service().create(command);

		assertThat(result.merchantId()).isNull();
		verify(merchantResolutionPort, never()).resolve(anyString());
	}

	@Test
	void throwsAndRecordsAuditForInvalidCurrency() {
		CreateTransactionCommand command = new CreateTransactionCommand("EXT-001", CUSTOMER_ID, "MOCK_BANK_A",
				new BigDecimal("125.50"), "zar", "DEBIT", "desc", null, Instant.parse("2026-08-06T08:00:00Z"));

		assertThatThrownBy(() -> service().create(command)).isInstanceOf(TransactionValidationException.class);

		ArgumentCaptor<RecordAuditEventCommand> auditCaptor = ArgumentCaptor.forClass(RecordAuditEventCommand.class);
		verify(recordAuditEventUseCase).record(auditCaptor.capture());
		assertThat(auditCaptor.getValue().eventType()).isEqualTo("TRANSACTION_VALIDATION_FAILED");
		verify(transactionSourceRepositoryPort, never()).findByCode(anyString());
	}

	@Test
	void throwsAndRecordsAuditForNegativeAmountWithoutAttemptingSourceResolution() {
		CreateTransactionCommand command = new CreateTransactionCommand("EXT-001", CUSTOMER_ID, "MOCK_BANK_A",
				new BigDecimal("-10.00"), "ZAR", "DEBIT", "desc", null, Instant.parse("2026-08-06T08:00:00Z"));

		assertThatThrownBy(() -> service().create(command)).isInstanceOf(TransactionValidationException.class);

		ArgumentCaptor<RecordAuditEventCommand> auditCaptor = ArgumentCaptor.forClass(RecordAuditEventCommand.class);
		verify(recordAuditEventUseCase).record(auditCaptor.capture());
		assertThat(auditCaptor.getValue().eventType()).isEqualTo("TRANSACTION_VALIDATION_FAILED");
		verify(transactionSourceRepositoryPort, never()).findByCode(anyString());
		verify(categoriseTransactionUseCase, never()).categorise(any());
	}

	@Test
	void throwsAndRecordsAuditForInvalidDirection() {
		CreateTransactionCommand command = new CreateTransactionCommand("EXT-001", CUSTOMER_ID, "MOCK_BANK_A",
				new BigDecimal("125.50"), "ZAR", "SIDEWAYS", "desc", null, Instant.parse("2026-08-06T08:00:00Z"));

		assertThatThrownBy(() -> service().create(command)).isInstanceOf(TransactionValidationException.class);

		ArgumentCaptor<RecordAuditEventCommand> auditCaptor = ArgumentCaptor.forClass(RecordAuditEventCommand.class);
		verify(recordAuditEventUseCase).record(auditCaptor.capture());
		assertThat(auditCaptor.getValue().eventType()).isEqualTo("TRANSACTION_VALIDATION_FAILED");
	}

	@Test
	void throwsAndRecordsAuditWhenSourceNotFound() {
		when(transactionSourceRepositoryPort.findByCode("MOCK_BANK_A")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().create(aCommand())).isInstanceOf(TransactionSourceNotFoundException.class);

		ArgumentCaptor<RecordAuditEventCommand> auditCaptor = ArgumentCaptor.forClass(RecordAuditEventCommand.class);
		verify(recordAuditEventUseCase).record(auditCaptor.capture());
		assertThat(auditCaptor.getValue().eventType()).isEqualTo("TRANSACTION_SOURCE_NOT_FOUND");
		verify(customerExistsPort, never()).exists(any());
	}

	@Test
	void throwsWhenSourceIsInactive() {
		TransactionSource inactiveSource = TransactionSource.register(TransactionSourceId.generate(), "MOCK_BANK_A",
				"Mock Bank A", SourceStatus.INACTIVE, FIXED_CLOCK);
		when(transactionSourceRepositoryPort.findByCode("MOCK_BANK_A")).thenReturn(Optional.of(inactiveSource));

		assertThatThrownBy(() -> service().create(aCommand())).isInstanceOf(TransactionSourceNotFoundException.class);
	}

	@Test
	void throwsAndRecordsAuditWhenCustomerNotFound() {
		TransactionSource source = activeSource();
		when(transactionSourceRepositoryPort.findByCode("MOCK_BANK_A")).thenReturn(Optional.of(source));
		when(customerExistsPort.exists(CUSTOMER_ID)).thenReturn(false);

		assertThatThrownBy(() -> service().create(aCommand())).isInstanceOf(CustomerNotFoundException.class);

		ArgumentCaptor<RecordAuditEventCommand> auditCaptor = ArgumentCaptor.forClass(RecordAuditEventCommand.class);
		verify(recordAuditEventUseCase).record(auditCaptor.capture());
		assertThat(auditCaptor.getValue().eventType()).isEqualTo("TRANSACTION_CUSTOMER_NOT_FOUND");
		verify(transactionRepositoryPort, never()).findBySourceIdAndExternalTransactionId(any(), anyString());
	}

	@Test
	void throwsAndRecordsAuditWhenDuplicateFoundByPreCheck() {
		TransactionSource source = activeSource();
		when(transactionSourceRepositoryPort.findByCode("MOCK_BANK_A")).thenReturn(Optional.of(source));
		when(customerExistsPort.exists(CUSTOMER_ID)).thenReturn(true);
		Transaction existing = Transaction.register(TransactionId.generate(), CUSTOMER_ID, source.id(), "EXT-001",
				null, CATEGORY_ID, new Money(new BigDecimal("10.00"), "ZAR"), TransactionDirection.DEBIT, "desc",
				Instant.parse("2026-08-06T08:00:00Z"), FIXED_CLOCK);
		when(transactionRepositoryPort.findBySourceIdAndExternalTransactionId(source.id(), "EXT-001"))
				.thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> service().create(aCommand())).isInstanceOf(DuplicateTransactionException.class);

		ArgumentCaptor<RecordAuditEventCommand> auditCaptor = ArgumentCaptor.forClass(RecordAuditEventCommand.class);
		verify(recordAuditEventUseCase).record(auditCaptor.capture());
		assertThat(auditCaptor.getValue().eventType()).isEqualTo("TRANSACTION_DUPLICATE_REJECTED");
		verify(merchantResolutionPort, never()).resolve(anyString());
		verify(categoriseTransactionUseCase, never()).categorise(any());
	}

	@Test
	void throwsAndRecordsAuditWhenDuplicateDetectedAtPersistTime() {
		TransactionSource source = activeSource();
		when(transactionSourceRepositoryPort.findByCode("MOCK_BANK_A")).thenReturn(Optional.of(source));
		when(customerExistsPort.exists(CUSTOMER_ID)).thenReturn(true);
		when(transactionRepositoryPort.findBySourceIdAndExternalTransactionId(source.id(), "EXT-001"))
				.thenReturn(Optional.empty());
		when(categoriseTransactionUseCase.categorise(any(CategorisationInput.class)))
				.thenReturn(new CategorisationDecision(CATEGORY_ID, null, "fallback"));
		when(transactionRepositoryPort.save(any(Transaction.class)))
				.thenThrow(new DuplicateTransactionException(source.id().value(), "EXT-001"));

		assertThatThrownBy(() -> service().create(aCommand())).isInstanceOf(DuplicateTransactionException.class);

		ArgumentCaptor<RecordAuditEventCommand> auditCaptor = ArgumentCaptor.forClass(RecordAuditEventCommand.class);
		verify(recordAuditEventUseCase).record(auditCaptor.capture());
		assertThat(auditCaptor.getValue().eventType()).isEqualTo("TRANSACTION_DUPLICATE_REJECTED");
	}

}
