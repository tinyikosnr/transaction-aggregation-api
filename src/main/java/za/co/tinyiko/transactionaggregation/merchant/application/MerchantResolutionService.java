package za.co.tinyiko.transactionaggregation.merchant.application;

import java.time.Clock;

import org.springframework.stereotype.Service;

import za.co.tinyiko.transactionaggregation.merchant.domain.Merchant;
import za.co.tinyiko.transactionaggregation.merchant.domain.MerchantId;
import za.co.tinyiko.transactionaggregation.merchant.domain.MerchantNormaliser;
import za.co.tinyiko.transactionaggregation.merchant.port.MerchantRepositoryPort;

@Service
class MerchantResolutionService implements MerchantResolutionPort {

	private final MerchantRepositoryPort merchantRepositoryPort;
	private final Clock clock;

	MerchantResolutionService(MerchantRepositoryPort merchantRepositoryPort, Clock clock) {
		this.merchantRepositoryPort = merchantRepositoryPort;
		this.clock = clock;
	}

	@Override
	public Merchant resolve(String rawMerchantName) {
		String normalisedName = MerchantNormaliser.normalise(rawMerchantName);

		return merchantRepositoryPort.findByNormalisedName(normalisedName)
				.orElseGet(() -> createNew(normalisedName, rawMerchantName.trim()));
	}

	private Merchant createNew(String normalisedName, String displayName) {
		Merchant merchant = Merchant.register(MerchantId.generate(), normalisedName, displayName, clock);
		try {
			return merchantRepositoryPort.save(merchant);
		} catch (DuplicateMerchantException e) {
			// A concurrent request created this merchant between our lookup and our save.
			// Resolution never fails outright for this reason - recover by returning the
			// one that now exists.
			return merchantRepositoryPort.findByNormalisedName(normalisedName).orElseThrow(() -> e);
		}
	}

}
