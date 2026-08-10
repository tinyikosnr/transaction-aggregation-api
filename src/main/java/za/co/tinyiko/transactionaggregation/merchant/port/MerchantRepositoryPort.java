package za.co.tinyiko.transactionaggregation.merchant.port;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import za.co.tinyiko.transactionaggregation.merchant.domain.Merchant;
import za.co.tinyiko.transactionaggregation.merchant.domain.MerchantId;

/**
 * Outbound port the merchant application layer uses to persist and read merchants (TDS 60).
 *
 * <p>{@code save}/{@code findByNormalisedName} are all {@code MerchantResolutionService} ever
 * needed. {@code findByIds}, added in {@code feature/transaction-query} for
 * {@code GetMerchantsUseCase}, is a single batch method rather than a {@code findById} +
 * {@code findByIds} pair - the single-item case just calls it with a one-element collection - and
 * must translate to one SQL {@code IN (...)} query, not a loop, so transaction search can enrich
 * a whole page of results with exactly one merchant lookup.
 */
public interface MerchantRepositoryPort {

	Merchant save(Merchant merchant);

	Optional<Merchant> findByNormalisedName(String normalisedName);

	List<Merchant> findByIds(Collection<MerchantId> ids);

}
