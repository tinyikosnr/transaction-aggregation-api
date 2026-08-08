package za.co.tinyiko.transactionaggregation.transaction.application;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import za.co.tinyiko.transactionaggregation.transaction.domain.Transaction;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionDirection;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionId;
import za.co.tinyiko.transactionaggregation.transaction.domain.TransactionSource;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionRepositoryPort;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionSourceRepositoryPort;

/**
 * Orchestrates single-transaction ingestion (TDS 51): validate, resolve source, validate
 * customer, detect duplicates, resolve merchant, categorise, persist, audit. One
 * {@code @Transactional} boundary for the whole use case - none of these calls are
 * external/network calls, they're all in-process calls to other Spring-managed services within
 * this same modular monolith, so nothing here violates "no long-running/external calls inside a
 * database transaction."
 *
 * <p>{@code correlationId} comes from the command (SAD 34.10) - resolved upstream by
 * {@code config.CorrelationIdFilter} from the inbound request, not generated here, so the same
 * id ends up on both the HTTP response and this transaction's audit trail. {@code actor} also
 * comes from the command (SAD 36, TDS's audit field catalogue) - the JWT subject of the
 * authenticated caller, resolved upstream in {@code api.controller} - replacing the earlier
 * {@code "SYSTEM"} placeholder now that real authenticated identity exists (see
 * {@link CreateTransactionCommand}'s own Javadoc for why this module never sees a JWT/Authentication
 * type directly).
 */
@Service
class CreateTransactionService implements CreateTransactionUseCase {

	private static final String AGGREGATE_TYPE = "TRANSACTION";

	private final TransactionRepositoryPort transactionRepositoryPort;
	private final TransactionSourceRepositoryPort transactionSourceRepositoryPort;
	private final CustomerExistsPort customerExistsPort;
	private final MerchantResolutionPort merchantResolutionPort;
	private final CategoriseTransactionUseCase categoriseTransactionUseCase;
	private final GetCategoryUseCase getCategoryUseCase;
	private final RecordAuditEventUseCase recordAuditEventUseCase;
	private final Clock clock;
	private final ObjectMapper objectMapper;

	CreateTransactionService(
			TransactionRepositoryPort transactionRepositoryPort,
			TransactionSourceRepositoryPort transactionSourceRepositoryPort,
			CustomerExistsPort customerExistsPort,
			MerchantResolutionPort merchantResolutionPort,
			CategoriseTransactionUseCase categoriseTransactionUseCase,
			GetCategoryUseCase getCategoryUseCase,
			RecordAuditEventUseCase recordAuditEventUseCase,
			Clock clock,
			ObjectMapper objectMapper
	) {
		this.transactionRepositoryPort = transactionRepositoryPort;
		this.transactionSourceRepositoryPort = transactionSourceRepositoryPort;
		this.customerExistsPort = customerExistsPort;
		this.merchantResolutionPort = merchantResolutionPort;
		this.categoriseTransactionUseCase = categoriseTransactionUseCase;
		this.getCategoryUseCase = getCategoryUseCase;
		this.recordAuditEventUseCase = recordAuditEventUseCase;
		this.clock = clock;
		this.objectMapper = objectMapper;
	}

	@Override
	@Transactional
	public TransactionCreatedResult create(CreateTransactionCommand command) {
		TransactionId transactionId = TransactionId.generate();
		CorrelationId correlationId = command.correlationId();

		Money amount = buildMoney(command, transactionId, correlationId);
		TransactionDirection direction = parseDirection(command, transactionId, correlationId);

		TransactionSource source = resolveSource(command, transactionId, correlationId);
		validateCustomer(command, transactionId, correlationId);
		checkForDuplicate(command, source, transactionId, correlationId);

		MerchantResolutionResult merchant = resolveMerchant(command);
		CategorisationDecision decision = categorise(command, merchant, direction);

		Transaction transaction = registerTransaction(command, transactionId, source, merchant, decision, amount, direction, correlationId);
		Transaction saved = persist(transaction, source, command, correlationId);

		recordSuccess(saved, decision, correlationId, command.actor());

		CategoryView category = getCategoryUseCase.get(decision.categoryId());
		return toResult(saved, source, merchant, category);
	}

	private TransactionCreatedResult toResult(Transaction transaction, TransactionSource source, MerchantResolutionResult merchant, CategoryView category) {
		return new TransactionCreatedResult(
				transaction.id().value(),
				transaction.customerId(),
				source.code(),
				transaction.externalTransactionId(),
				merchant == null ? null : merchant.merchantId(),
				merchant == null ? null : merchant.displayName(),
				category.code(),
				category.name(),
				transaction.amount().amount(),
				transaction.amount().currency(),
				transaction.direction().name(),
				transaction.description(),
				transaction.status().name(),
				transaction.occurredAt(),
				transaction.receivedAt(),
				transaction.createdAt());
	}

	private Money buildMoney(CreateTransactionCommand command, TransactionId transactionId, CorrelationId correlationId) {
		try {
			Money money = new Money(command.amount(), command.currency());
			if (money.amount().signum() <= 0) {
				throw new IllegalArgumentException("amount must be greater than zero");
			}
			return money;
		} catch (IllegalArgumentException e) {
			recordFailure(transactionId, "TRANSACTION_VALIDATION_FAILED", correlationId, command.actor(), e.getMessage());
			throw new TransactionValidationException(e.getMessage(), e);
		}
	}

	private TransactionDirection parseDirection(CreateTransactionCommand command, TransactionId transactionId, CorrelationId correlationId) {
		try {
			return TransactionDirection.valueOf(command.direction());
		} catch (IllegalArgumentException e) {
			recordFailure(transactionId, "TRANSACTION_VALIDATION_FAILED", correlationId, command.actor(), e.getMessage());
			throw new TransactionValidationException("direction must be CREDIT or DEBIT", e);
		}
	}

	private TransactionSource resolveSource(CreateTransactionCommand command, TransactionId transactionId, CorrelationId correlationId) {
		Optional<TransactionSource> source = transactionSourceRepositoryPort.findByCode(command.sourceCode());
		if (source.isEmpty() || !source.get().isActive()) {
			recordFailure(transactionId, "TRANSACTION_SOURCE_NOT_FOUND", correlationId, command.actor(),
					"source code: " + command.sourceCode());
			throw new TransactionSourceNotFoundException(command.sourceCode());
		}
		return source.get();
	}

	private void validateCustomer(CreateTransactionCommand command, TransactionId transactionId, CorrelationId correlationId) {
		if (!customerExistsPort.exists(command.customerId())) {
			recordFailure(transactionId, "TRANSACTION_CUSTOMER_NOT_FOUND", correlationId, command.actor(),
					"customer id: " + command.customerId());
			throw new CustomerNotFoundException(command.customerId());
		}
	}

	private void checkForDuplicate(CreateTransactionCommand command, TransactionSource source, TransactionId transactionId, CorrelationId correlationId) {
		boolean exists = transactionRepositoryPort
				.findBySourceIdAndExternalTransactionId(source.id(), command.externalTransactionId())
				.isPresent();
		if (exists) {
			recordFailure(transactionId, "TRANSACTION_DUPLICATE_REJECTED", correlationId, command.actor(),
					"source: " + command.sourceCode() + ", externalTransactionId: " + command.externalTransactionId());
			throw new DuplicateTransactionException(source.id().value(), command.externalTransactionId());
		}
	}

	private MerchantResolutionResult resolveMerchant(CreateTransactionCommand command) {
		if (command.merchantName() == null || command.merchantName().isBlank()) {
			return null;
		}
		return merchantResolutionPort.resolve(command.merchantName());
	}

	private CategorisationDecision categorise(CreateTransactionCommand command, MerchantResolutionResult merchant, TransactionDirection direction) {
		String merchantText = merchant == null ? null : merchant.displayName();
		CategorisationInput input = new CategorisationInput(merchantText, command.description(), direction.name());
		return categoriseTransactionUseCase.categorise(input);
	}

	private Transaction registerTransaction(
			CreateTransactionCommand command,
			TransactionId transactionId,
			TransactionSource source,
			MerchantResolutionResult merchant,
			CategorisationDecision decision,
			Money amount,
			TransactionDirection direction,
			CorrelationId correlationId
	) {
		try {
			return Transaction.register(
					transactionId,
					command.customerId(),
					source.id(),
					command.externalTransactionId(),
					merchant == null ? null : merchant.merchantId(),
					decision.categoryId(),
					amount,
					direction,
					command.description(),
					command.occurredAt(),
					clock);
		} catch (IllegalArgumentException e) {
			recordFailure(transactionId, "TRANSACTION_VALIDATION_FAILED", correlationId, command.actor(), e.getMessage());
			throw new TransactionValidationException(e.getMessage(), e);
		}
	}

	private Transaction persist(Transaction transaction, TransactionSource source, CreateTransactionCommand command, CorrelationId correlationId) {
		try {
			return transactionRepositoryPort.save(transaction);
		} catch (DuplicateTransactionException e) {
			recordFailure(transaction.id(), "TRANSACTION_DUPLICATE_REJECTED", correlationId, command.actor(),
					"source: " + source.code() + ", externalTransactionId: " + command.externalTransactionId());
			throw e;
		}
	}

	private void recordSuccess(Transaction transaction, CategorisationDecision decision, CorrelationId correlationId, String actor) {
		Map<String, Object> eventData = new HashMap<>();
		eventData.put("categoryId", decision.categoryId().toString());
		eventData.put("matchedRuleId", decision.matchedRuleId() == null ? null : decision.matchedRuleId().toString());
		eventData.put("reason", decision.reason());
		recordAuditEventUseCase.record(new RecordAuditEventCommand(
				AGGREGATE_TYPE, transaction.id().value(), "TRANSACTION_CREATED", actor, correlationId, toJson(eventData)));
	}

	private void recordFailure(TransactionId transactionId, String eventType, CorrelationId correlationId, String actor, String reason) {
		Map<String, Object> eventData = new HashMap<>();
		eventData.put("reason", reason);
		recordAuditEventUseCase.record(new RecordAuditEventCommand(
				AGGREGATE_TYPE, transactionId.value(), eventType, actor, correlationId, toJson(eventData)));
	}

	private String toJson(Map<String, Object> data) {
		return objectMapper.writeValueAsString(data);
	}

}
