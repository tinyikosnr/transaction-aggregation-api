package za.co.tinyiko.transactionaggregation.merchant.application;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Inbound port to read already-known merchants by id - added in {@code feature/transaction-query}
 * for transaction get/search enrichment. Distinct from {@link MerchantResolutionPort}, which
 * resolves a raw merchant name (finding or creating a row as needed); this port never writes.
 *
 * <p>{@link #findByIds} must translate to a single SQL {@code IN (...)} query, not a loop, so a
 * page of up to 100 search results can be enriched with exactly one merchant lookup regardless of
 * page size.
 */
public interface GetMerchantsUseCase {

	Optional<MerchantView> get(UUID merchantId);

	List<MerchantView> findByIds(Collection<UUID> merchantIds);

}
