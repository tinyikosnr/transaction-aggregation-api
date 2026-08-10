package za.co.tinyiko.transactionaggregation.merchant.application;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import za.co.tinyiko.transactionaggregation.merchant.domain.Merchant;
import za.co.tinyiko.transactionaggregation.merchant.domain.MerchantId;
import za.co.tinyiko.transactionaggregation.merchant.port.MerchantRepositoryPort;

@Service
class GetMerchantsService implements GetMerchantsUseCase {

	private final MerchantRepositoryPort merchantRepositoryPort;

	GetMerchantsService(MerchantRepositoryPort merchantRepositoryPort) {
		this.merchantRepositoryPort = merchantRepositoryPort;
	}

	@Override
	public Optional<MerchantView> get(UUID merchantId) {
		return merchantRepositoryPort.findByIds(List.of(new MerchantId(merchantId))).stream()
				.findFirst()
				.map(GetMerchantsService::toView);
	}

	@Override
	public List<MerchantView> findByIds(Collection<UUID> merchantIds) {
		List<MerchantId> typedIds = merchantIds.stream().map(MerchantId::new).toList();
		return merchantRepositoryPort.findByIds(typedIds).stream()
				.map(GetMerchantsService::toView)
				.toList();
	}

	private static MerchantView toView(Merchant merchant) {
		return new MerchantView(merchant.id().value(), merchant.displayName());
	}

}
