package za.co.tinyiko.transactionaggregation.merchant.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import za.co.tinyiko.transactionaggregation.merchant.application.DuplicateMerchantException;
import za.co.tinyiko.transactionaggregation.merchant.domain.Merchant;
import za.co.tinyiko.transactionaggregation.merchant.domain.MerchantId;
import za.co.tinyiko.transactionaggregation.merchant.mapper.MerchantMapper;
import za.co.tinyiko.transactionaggregation.merchant.port.MerchantRepositoryPort;

@Component
class JpaMerchantRepositoryAdapter implements MerchantRepositoryPort {

	private final SpringDataMerchantRepository springDataMerchantRepository;

	JpaMerchantRepositoryAdapter(SpringDataMerchantRepository springDataMerchantRepository) {
		this.springDataMerchantRepository = springDataMerchantRepository;
	}

	@Override
	@Transactional
	public Merchant save(Merchant merchant) {
		MerchantEntity entity = springDataMerchantRepository.findById(merchant.id().value())
				.orElseGet(MerchantEntity::new);
		MerchantMapper.applyTo(entity, merchant);
		try {
			// saveAndFlush, not save: the unique-constraint violation must surface here,
			// synchronously, to be caught - a deferred flush would let it escape uncaught
			// later, outside this method, at transaction commit.
			MerchantEntity saved = springDataMerchantRepository.saveAndFlush(entity);
			return MerchantMapper.toDomain(saved);
		} catch (DataIntegrityViolationException e) {
			throw new DuplicateMerchantException(merchant.normalisedName(), e);
		}
	}

	@Override
	public Optional<Merchant> findByNormalisedName(String normalisedName) {
		return springDataMerchantRepository.findByNormalisedName(normalisedName)
				.map(MerchantMapper::toDomain);
	}

	@Override
	public List<Merchant> findByIds(Collection<MerchantId> ids) {
		List<UUID> rawIds = ids.stream().map(MerchantId::value).toList();
		return springDataMerchantRepository.findAllById(rawIds).stream()
				.map(MerchantMapper::toDomain)
				.toList();
	}

}
