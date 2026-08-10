package za.co.tinyiko.transactionaggregation.transaction.application;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;

/**
 * Orchestrates bulk transaction ingestion (FR-02, UC-02, SAD 32.5/35.2/39.8, TDS 29) by calling
 * the existing {@link CreateTransactionUseCase} once per item, sequentially - not a copy of
 * {@code CreateTransactionService}'s workflow, a reuse of it.
 *
 * <p><strong>No {@code @Transactional} on this class, deliberately.</strong> {@link
 * CreateTransactionUseCase#create} keeps its own {@code @Transactional} (default {@code REQUIRED}
 * propagation) on {@code CreateTransactionService}, unchanged. Because this method carries no
 * transaction of its own, each call to {@code createTransactionUseCase.create(...)} - made
 * through the <em>injected</em> {@link CreateTransactionUseCase} proxy bean, a genuine
 * bean-to-bean call, not self-invocation - opens a brand-new, independent physical transaction.
 * When an item throws, Spring's transactional interceptor rolls back and commits that rollback
 * <em>before</em> re-throwing, so {@link #processOne} always catches the exception after that
 * item's own transaction is already cleanly gone; every previously-processed item's transaction
 * already committed independently and is unaffected (SAD 32.5: "Bulk ingestion processes each
 * item independently to support partial success").
 *
 * <p>Same-batch duplicates (two items sharing the same source/externalTransactionId) require no
 * special detection: processing is strictly sequential, so the first occurrence commits before
 * the second is even attempted, and the second's existing pre-check/unique-constraint duplicate
 * detection (inside {@code CreateTransactionService}) sees it exactly as it would see a duplicate
 * against already-persisted data.
 *
 * <p>Only the four documented, expected per-item failure types are caught here - {@link
 * DuplicateTransactionException}, {@link CustomerNotFoundException}, {@link
 * TransactionSourceNotFoundException}, {@link TransactionValidationException}. Nothing broader:
 * SAD 32.5/39.8 describe an "invalid item" (a business/validation-shaped failure of that item's
 * own data), never a systemic condition, and 39.8's own "must not return a successful overall
 * status that hides item-level failures" cuts against silently folding an infrastructure failure
 * (e.g. the database becoming unavailable partway through the batch) into an ordinary-looking
 * per-item result. An unexpected exception on one item is allowed to propagate out of {@link
 * #create}, aborting the whole bulk request with the existing whole-request 500/503 handling
 * ({@code api.advice.GlobalExceptionHandler}) - items already processed before that point stay
 * committed (each was its own already-completed transaction), the client just receives no 207
 * body for the aborted remainder and must reconcile via {@code GET /api/v1/transactions}.
 */
@Service
class BulkCreateTransactionsService implements CreateTransactionsBulkUseCase {

	private final CreateTransactionUseCase createTransactionUseCase;

	BulkCreateTransactionsService(CreateTransactionUseCase createTransactionUseCase) {
		this.createTransactionUseCase = createTransactionUseCase;
	}

	@Override
	public BulkTransactionResult create(BulkCreateTransactionsCommand command) {
		List<BulkTransactionItemResult> results = new ArrayList<>(command.items().size());
		int successful = 0;
		for (BulkTransactionItemInput item : command.items()) {
			BulkTransactionItemResult result = processOne(item, command.correlationId(), command.actor());
			results.add(result);
			if (result.created()) {
				successful++;
			}
		}
		return new BulkTransactionResult(results.size(), successful, results.size() - successful, results);
	}

	private BulkTransactionItemResult processOne(BulkTransactionItemInput item, CorrelationId correlationId, String actor) {
		try {
			CreateTransactionCommand command = toCommand(item, correlationId, actor);
			TransactionCreatedResult result = createTransactionUseCase.create(command);
			return BulkTransactionItemResult.created(result.id());
		} catch (DuplicateTransactionException e) {
			return BulkTransactionItemResult.failed("TRANSACTION_DUPLICATE", e.getMessage());
		} catch (CustomerNotFoundException e) {
			return BulkTransactionItemResult.failed("CUSTOMER_NOT_FOUND", e.getMessage());
		} catch (TransactionSourceNotFoundException e) {
			return BulkTransactionItemResult.failed("SOURCE_NOT_FOUND", e.getMessage());
		} catch (TransactionValidationException e) {
			return BulkTransactionItemResult.failed("REQUEST_VALIDATION_FAILED", e.getMessage());
		}
	}

	/**
	 * Validates the raw, per-item {@link BulkTransactionItemInput} explicitly and translates a
	 * missing required field into a {@link TransactionValidationException} - deliberately not
	 * relying on {@link CreateTransactionCommand}'s own {@code Objects.requireNonNull} compact
	 * constructor, whose {@link NullPointerException} is not one of the exception types {@link
	 * #processOne} catches (and should not become one: an NPE is reserved for programming errors
	 * elsewhere in this codebase, not expected per-item input handling). Amount positivity,
	 * currency format, direction validity, externalTransactionId length/blankness, description
	 * length and occurredAt future-tolerance are intentionally left to {@code
	 * CreateTransactionService}/{@code Money}/{@code Transaction.register}, exactly as for
	 * single-create - those already convert to {@link TransactionValidationException} without any
	 * help from this method.
	 */
	private CreateTransactionCommand toCommand(BulkTransactionItemInput item, CorrelationId correlationId, String actor) {
		requireNonNull(item.externalTransactionId(), "externalTransactionId");
		requireNonNull(item.customerId(), "customerId");
		requireNonNull(item.sourceCode(), "sourceCode");
		requireNonNull(item.amount(), "amount");
		requireNonNull(item.currency(), "currency");
		requireNonNull(item.direction(), "direction");
		requireNonNull(item.occurredAt(), "occurredAt");
		return new CreateTransactionCommand(
				item.externalTransactionId(),
				item.customerId(),
				item.sourceCode(),
				item.amount(),
				item.currency(),
				item.direction(),
				item.description(),
				item.merchantName(),
				item.occurredAt(),
				correlationId,
				actor);
	}

	private void requireNonNull(Object value, String fieldName) {
		if (value == null) {
			throw new TransactionValidationException(fieldName + " must not be null", null);
		}
	}

}
