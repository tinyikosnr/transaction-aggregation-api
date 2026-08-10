package za.co.tinyiko.transactionaggregation.transaction.application;

import java.util.List;

/**
 * The complete outcome of {@link CreateTransactionsBulkUseCase#create} (SAD 35.2): every field
 * the documented response needs, computed once, entirely within {@code transaction.application} -
 * {@code total}/{@code successful}/{@code failed} span the whole submitted batch, never a subset,
 * since {@code BulkCreateTransactionsService} processes every submitted item (never filters one
 * out before this point; see {@link BulkTransactionItemInput}'s own Javadoc). {@code results} is
 * in the same order as the {@link BulkCreateTransactionsCommand#items()} list it was built from -
 * the caller does not need to reconcile order or indexes itself.
 */
public record BulkTransactionResult(int total, int successful, int failed, List<BulkTransactionItemResult> results) {
}
