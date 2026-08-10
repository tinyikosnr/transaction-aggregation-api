package za.co.tinyiko.transactionaggregation.transaction.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.ObjectMapper;

import za.co.tinyiko.transactionaggregation.audit.application.RecordAuditEventCommand;
import za.co.tinyiko.transactionaggregation.audit.application.RecordAuditEventUseCase;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategorisationDecision;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategorisationInput;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategoriseTransactionUseCase;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategoryView;
import za.co.tinyiko.transactionaggregation.categorisation.application.GetCategoryUseCase;
import za.co.tinyiko.transactionaggregation.customer.application.CustomerExistsPort;
import za.co.tinyiko.transactionaggregation.merchant.application.MerchantResolutionPort;
import za.co.tinyiko.transactionaggregation.merchant.application.MerchantResolutionResult;
import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;
import za.co.tinyiko.transactionaggregation.transaction.domain.Money;
import za.co.tinyiko.transactionaggregation.transaction.domain.SourceStatus;
import za.co.tinyiko.transactionaggregation.transaction.domain.Transaction;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionDirection;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionId;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionSource;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionSourceId;
import za.co.tinyiko.transactionaggregation.transaction.port.RejectionReason;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionMetricsPort;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionRepositoryPort;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSourceRepositoryPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTransactionServiceTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-06T08:15:00Z"), ZoneOffset.UTC);
	private static final UUID CUSTOMER_ID = UUID.randomUUID();
	private static final UUID CATEGORY_ID = UUID.randomUUID();
	private static final UUID MERCHANT_ID = UUID.randomUUID();
	private static final CorrelationId CORRELATION_ID = new CorrelationId("test-correlation-id");
	private static final CategoryView CATEGORY_VIEW = new CategoryView(CATEGORY_ID, "GROCERIES", "Groceries");
	private static final String ACTOR = "jwt-subject-001";

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
	private GetCategoryUseCase getCategoryUseCase;
	@Mock
	private RecordAuditEventUseCase recordAuditEventUseCase;
	@Mock
	private TransactionMetricsPort metrics;
	@Mock
	private TransactionMetricsPort.ProcessingTimer processingTimer;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		when(metrics.startProcessingTimer()).thenReturn(processingTimer);
	}

	private CreateTransactionService service() {
		return new CreateTransactionService(transactionRepositoryPort, transactionSourceRepositoryPort,
				customerExistsPort, merchantResolutionPort, categoriseTransactionUseCase, getCategoryUseCase,
				recordAuditEventUseCase, FIXED_CLOCK, objectMapper, metrics);
	}

	private static TransactionSource activeSource() {
		return TransactionSource.register(TransactionSourceId.generate(), "MOCK_BANK_A", "Mock Bank A",
				SourceStatus.ACTIVE, FIXED_CLOCK);
	}

	private static CreateTransactionCommand aCommand() {
		return new CreateTransactionCommand("EXT-001", CUSTOMER_ID, "MOCK_BANK_A", new BigDecimal("125.50"), "ZAR",
				"DEBIT", "Checkers Centurion", "Checkers", Instant.parse("2026-08-06T08:00:00Z"), CORRELATION_ID, ACTOR);
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
				.thenReturn(new CategorisationDecision(CATEGORY_ID, UUID.randomUUID(), "Merchant contained CHECKERS", false));
		when(getCategoryUseCase.get(CATEGORY_ID)).thenReturn(CATEGORY_VIEW);
		when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TransactionCreatedResult result = service().create(aCommand());

		assertThat(result.customerId()).isEqualTo(CUSTOMER_ID);
		assertThat(result.categoryCode()).isEqualTo("GROCERIES");
		assertThat(result.categoryName()).isEqualTo("Groceries");
		assertThat(result.merchantId()).isEqualTo(MERCHANT_ID);
		assertThat(result.merchantDisplayName()).isEqualTo("Checkers");
		assertThat(result.sourceCode()).isEqualTo("MOCK_BANK_A");

		ArgumentCaptor<RecordAuditEventCommand> auditCaptor = ArgumentCaptor.forClass(RecordAuditEventCommand.class);
		verify(recordAuditEventUseCase).record(auditCaptor.capture());
		assertThat(auditCaptor.getValue().eventType()).isEqualTo("TRANSACTION_CREATED");
		assertThat(auditCaptor.getValue().correlationId()).isEqualTo(CORRELATION_ID);
		assertThat(auditCaptor.getValue().actor()).isEqualTo(ACTOR);

		verify(metrics).transactionReceived();
		verify(metrics).transactionProcessed();
		verify(metrics, never()).categorisationFallback();
		verify(metrics, never()).transactionRejected(any());
		verify(metrics, never()).transactionDuplicateRejected();
		verify(processingTimer).stop();
	}

	/**
	 * {@code transactions.processed}/{@code transactions.categorisation.fallback} must only be
	 * incremented <em>after</em> the success-audit recording itself has already completed - proves
	 * the exact ordering the design requires, not just that both eventually get called.
	 */
	@Test
	void incrementsProcessedAndFallbackMetricsOnlyAfterSuccessAuditRecordingCompletes() {
		TransactionSource source = activeSource();
		when(transactionSourceRepositoryPort.findByCode("MOCK_BANK_A")).thenReturn(Optional.of(source));
		when(customerExistsPort.exists(CUSTOMER_ID)).thenReturn(true);
		when(transactionRepositoryPort.findBySourceIdAndExternalTransactionId(source.id(), "EXT-001"))
				.thenReturn(Optional.empty());
		when(merchantResolutionPort.resolve("Checkers")).thenReturn(new MerchantResolutionResult(MERCHANT_ID, "Checkers"));
		when(categoriseTransactionUseCase.categorise(any(CategorisationInput.class)))
				.thenReturn(new CategorisationDecision(CATEGORY_ID, UUID.randomUUID(), "No specific rule matched; fallback", true));
		when(getCategoryUseCase.get(CATEGORY_ID)).thenReturn(CATEGORY_VIEW);
		when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service().create(aCommand());

		verify(metrics).transactionProcessed();
		verify(metrics).categorisationFallback();

		InOrder order = inOrder(recordAuditEventUseCase, metrics);
		order.verify(recordAuditEventUseCase).record(any());
		order.verify(metrics).transactionProcessed();
		order.verify(metrics).categorisationFallback();
	}

	/**
	 * The exact risk the success-metric-placement correction guards against: persistence already
	 * succeeded, but the success-audit write itself fails and propagates - the workflow never
	 * reached its completed normal success path, so neither success counter may increment, even
	 * though the row was physically saved by the (mocked) repository. The timer still stops.
	 */
	@Test
	void doesNotIncrementProcessedOrFallbackMetricsWhenSuccessAuditRecordingFails() {
		TransactionSource source = activeSource();
		when(transactionSourceRepositoryPort.findByCode("MOCK_BANK_A")).thenReturn(Optional.of(source));
		when(customerExistsPort.exists(CUSTOMER_ID)).thenReturn(true);
		when(transactionRepositoryPort.findBySourceIdAndExternalTransactionId(source.id(), "EXT-001"))
				.thenReturn(Optional.empty());
		when(merchantResolutionPort.resolve("Checkers")).thenReturn(new MerchantResolutionResult(MERCHANT_ID, "Checkers"));
		when(categoriseTransactionUseCase.categorise(any(CategorisationInput.class)))
				.thenReturn(new CategorisationDecision(CATEGORY_ID, UUID.randomUUID(), "Merchant contained CHECKERS", true));
		when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
		doThrow(new RuntimeException("audit backend unavailable")).when(recordAuditEventUseCase).record(any());

		assertThatThrownBy(() -> service().create(aCommand())).isInstanceOf(RuntimeException.class);

		verify(metrics, never()).transactionProcessed();
		verify(metrics, never()).categorisationFallback();
		verify(processingTimer).stop();
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
				.thenReturn(new CategorisationDecision(CATEGORY_ID, null, "fallback", true));
		when(getCategoryUseCase.get(CATEGORY_ID)).thenReturn(CATEGORY_VIEW);
		when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CreateTransactionCommand command = new CreateTransactionCommand("EXT-001", CUSTOMER_ID, "MOCK_BANK_A",
				new BigDecimal("125.50"), "ZAR", "DEBIT", "pharmacy purchase", "Dis-Chem Pharmacy",
				Instant.parse("2026-08-06T08:00:00Z"), CORRELATION_ID, ACTOR);

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
				.thenReturn(new CategorisationDecision(CATEGORY_ID, null, "fallback", true));
		when(getCategoryUseCase.get(CATEGORY_ID)).thenReturn(CATEGORY_VIEW);
		when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CreateTransactionCommand command = new CreateTransactionCommand("EXT-001", CUSTOMER_ID, "MOCK_BANK_A",
				new BigDecimal("125.50"), "ZAR", "DEBIT", "unknown", " ", Instant.parse("2026-08-06T08:00:00Z"), CORRELATION_ID, ACTOR);

		TransactionCreatedResult result = service().create(command);

		assertThat(result.merchantId()).isNull();
		assertThat(result.merchantDisplayName()).isNull();
		verify(merchantResolutionPort, never()).resolve(anyString());
	}

	@Test
	void throwsAndRecordsAuditForInvalidCurrency() {
		CreateTransactionCommand command = new CreateTransactionCommand("EXT-001", CUSTOMER_ID, "MOCK_BANK_A",
				new BigDecimal("125.50"), "zar", "DEBIT", "desc", null, Instant.parse("2026-08-06T08:00:00Z"), CORRELATION_ID, ACTOR);

		assertThatThrownBy(() -> service().create(command)).isInstanceOf(TransactionValidationException.class);

		ArgumentCaptor<RecordAuditEventCommand> auditCaptor = ArgumentCaptor.forClass(RecordAuditEventCommand.class);
		verify(recordAuditEventUseCase).record(auditCaptor.capture());
		assertThat(auditCaptor.getValue().eventType()).isEqualTo("TRANSACTION_VALIDATION_FAILED");
		verify(transactionSourceRepositoryPort, never()).findByCode(anyString());

		verify(metrics).transactionReceived();
		verify(metrics).transactionRejected(RejectionReason.VALIDATION_FAILED);
		verify(metrics, never()).transactionProcessed();
		verify(metrics, never()).categorisationFallback();
		verify(processingTimer).stop();
	}

	@Test
	void throwsAndRecordsAuditForNegativeAmountWithoutAttemptingSourceResolution() {
		CreateTransactionCommand command = new CreateTransactionCommand("EXT-001", CUSTOMER_ID, "MOCK_BANK_A",
				new BigDecimal("-10.00"), "ZAR", "DEBIT", "desc", null, Instant.parse("2026-08-06T08:00:00Z"), CORRELATION_ID, ACTOR);

		assertThatThrownBy(() -> service().create(command)).isInstanceOf(TransactionValidationException.class);

		ArgumentCaptor<RecordAuditEventCommand> auditCaptor = ArgumentCaptor.forClass(RecordAuditEventCommand.class);
		verify(recordAuditEventUseCase).record(auditCaptor.capture());
		assertThat(auditCaptor.getValue().eventType()).isEqualTo("TRANSACTION_VALIDATION_FAILED");
		verify(transactionSourceRepositoryPort, never()).findByCode(anyString());
		verify(categoriseTransactionUseCase, never()).categorise(any());

		verify(metrics).transactionRejected(RejectionReason.VALIDATION_FAILED);
		verify(processingTimer).stop();
	}

	@Test
	void throwsAndRecordsAuditForInvalidDirection() {
		CreateTransactionCommand command = new CreateTransactionCommand("EXT-001", CUSTOMER_ID, "MOCK_BANK_A",
				new BigDecimal("125.50"), "ZAR", "SIDEWAYS", "desc", null, Instant.parse("2026-08-06T08:00:00Z"), CORRELATION_ID, ACTOR);

		assertThatThrownBy(() -> service().create(command)).isInstanceOf(TransactionValidationException.class);

		ArgumentCaptor<RecordAuditEventCommand> auditCaptor = ArgumentCaptor.forClass(RecordAuditEventCommand.class);
		verify(recordAuditEventUseCase).record(auditCaptor.capture());
		assertThat(auditCaptor.getValue().eventType()).isEqualTo("TRANSACTION_VALIDATION_FAILED");

		verify(metrics).transactionRejected(RejectionReason.VALIDATION_FAILED);
		verify(processingTimer).stop();
	}

	@Test
	void throwsAndRecordsAuditWhenSourceNotFound() {
		when(transactionSourceRepositoryPort.findByCode("MOCK_BANK_A")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().create(aCommand())).isInstanceOf(TransactionSourceNotFoundException.class);

		ArgumentCaptor<RecordAuditEventCommand> auditCaptor = ArgumentCaptor.forClass(RecordAuditEventCommand.class);
		verify(recordAuditEventUseCase).record(auditCaptor.capture());
		assertThat(auditCaptor.getValue().eventType()).isEqualTo("TRANSACTION_SOURCE_NOT_FOUND");
		verify(customerExistsPort, never()).exists(any());

		verify(metrics).transactionRejected(RejectionReason.SOURCE_NOT_FOUND);
		verify(metrics, never()).transactionProcessed();
		verify(processingTimer).stop();
	}

	@Test
	void throwsWhenSourceIsInactive() {
		TransactionSource inactiveSource = TransactionSource.register(TransactionSourceId.generate(), "MOCK_BANK_A",
				"Mock Bank A", SourceStatus.INACTIVE, FIXED_CLOCK);
		when(transactionSourceRepositoryPort.findByCode("MOCK_BANK_A")).thenReturn(Optional.of(inactiveSource));

		assertThatThrownBy(() -> service().create(aCommand())).isInstanceOf(TransactionSourceNotFoundException.class);

		verify(metrics).transactionRejected(RejectionReason.SOURCE_NOT_FOUND);
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

		verify(metrics).transactionRejected(RejectionReason.CUSTOMER_NOT_FOUND);
		verify(processingTimer).stop();
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

		verify(metrics).transactionDuplicateRejected();
		verify(metrics, never()).transactionRejected(any());
		verify(metrics, never()).transactionProcessed();
		verify(processingTimer).stop();
	}

	@Test
	void throwsAndRecordsAuditWhenDuplicateDetectedAtPersistTime() {
		TransactionSource source = activeSource();
		when(transactionSourceRepositoryPort.findByCode("MOCK_BANK_A")).thenReturn(Optional.of(source));
		when(customerExistsPort.exists(CUSTOMER_ID)).thenReturn(true);
		when(transactionRepositoryPort.findBySourceIdAndExternalTransactionId(source.id(), "EXT-001"))
				.thenReturn(Optional.empty());
		when(categoriseTransactionUseCase.categorise(any(CategorisationInput.class)))
				.thenReturn(new CategorisationDecision(CATEGORY_ID, null, "fallback", true));
		when(transactionRepositoryPort.save(any(Transaction.class)))
				.thenThrow(new DuplicateTransactionException(source.id().value(), "EXT-001"));

		assertThatThrownBy(() -> service().create(aCommand())).isInstanceOf(DuplicateTransactionException.class);

		ArgumentCaptor<RecordAuditEventCommand> auditCaptor = ArgumentCaptor.forClass(RecordAuditEventCommand.class);
		verify(recordAuditEventUseCase).record(auditCaptor.capture());
		assertThat(auditCaptor.getValue().eventType()).isEqualTo("TRANSACTION_DUPLICATE_REJECTED");

		verify(metrics).transactionDuplicateRejected();
		verify(metrics, never()).transactionProcessed();
		verify(metrics, never()).categorisationFallback();
		verify(processingTimer).stop();
	}

}
