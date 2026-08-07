/**
 * Transaction's public API: {@link za.co.tinyiko.transactionaggregation.transaction.application.CreateTransactionUseCase}
 * and {@link za.co.tinyiko.transactionaggregation.transaction.application.TransactionQueryPort},
 * plus their command/result shapes.
 *
 * <p>Exposed at the package level, per the pattern established for categorisation/audit/customer/
 * merchant - one {@code @NamedInterface} on the package, rather than annotating each exported
 * type individually. This is transaction's first real cross-module caller on the read side:
 * {@code aggregation} is the reason this file exists now rather than when {@code CreateTransactionUseCase}
 * was first built (nothing outside {@code transaction} called into it yet at that point).
 * {@code CreateTransactionService} and {@code TransactionQueryService} both stay internal
 * regardless, since both are package-private.
 */
@org.springframework.modulith.NamedInterface
package za.co.tinyiko.transactionaggregation.transaction.application;
