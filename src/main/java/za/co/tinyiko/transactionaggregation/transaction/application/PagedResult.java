package za.co.tinyiko.transactionaggregation.transaction.application;

import java.util.List;

/**
 * Framework-independent pagination wrapper - no Spring Data {@code Page}/{@code PagedModel}
 * ever crosses the {@code transaction.application} boundary. Lives here, not {@code shared}:
 * only {@code transaction} needs pagination today; per the established "promote to shared only
 * once a second real module consumes it" rule, this is not promoted preemptively.
 *
 * <p>Field names/shape match SAD 34.6's documented pagination envelope
 * ({@code content}/{@code page.number}/{@code page.size}/{@code page.totalElements}/
 * {@code page.totalPages}) so {@code api.mapper} can map this directly, one field at a time.
 */
public record PagedResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
}
