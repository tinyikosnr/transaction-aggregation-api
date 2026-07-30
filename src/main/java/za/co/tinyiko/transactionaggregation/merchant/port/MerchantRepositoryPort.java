package za.co.tinyiko.transactionaggregation.merchant.port;

import java.util.Optional;

import za.co.tinyiko.transactionaggregation.merchant.domain.Merchant;

/**
 * Outbound port the merchant application layer uses to persist and read merchants (TDS 60).
 *
 * <p>Deliberately minimal — only what {@code MerchantResolutionService} actually calls.
 * No {@code findById}/{@code existsById}: nothing currently consumes a by-id lookup for
 * Merchant, unlike {@code CustomerRepositoryPort}'s broader shape.
 */
public interface MerchantRepositoryPort {

	Merchant save(Merchant merchant);

	Optional<Merchant> findByNormalisedName(String normalisedName);

}
