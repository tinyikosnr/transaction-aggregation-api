package za.co.tinyiko.transactionaggregation.merchant.mapper;

import za.co.tinyiko.transactionaggregation.merchant.domain.Merchant;
import za.co.tinyiko.transactionaggregation.merchant.domain.MerchantId;
import za.co.tinyiko.transactionaggregation.merchant.persistence.MerchantEntity;

/**
 * Maps between the domain {@link Merchant} and the JPA {@link MerchantEntity}. Same
 * {@code toDomain}/{@code applyTo} shape as {@code customer.mapper.CustomerMapper}, for the
 * same reason: {@code applyTo} mutates an already-managed entity in place so Hibernate's own
 * version handling stays correct, rather than constructing a detached replacement.
 */
public final class MerchantMapper {

	private MerchantMapper() {
	}

	public static Merchant toDomain(MerchantEntity entity) {
		return Merchant.reconstitute(
				new MerchantId(entity.getId()),
				entity.getNormalisedName(),
				entity.getDisplayName(),
				entity.getCreatedAt(),
				entity.getUpdatedAt());
	}

	public static void applyTo(MerchantEntity entity, Merchant merchant) {
		entity.setId(merchant.id().value());
		entity.setNormalisedName(merchant.normalisedName());
		entity.setDisplayName(merchant.displayName());
		entity.setCreatedAt(merchant.createdAt());
		entity.setUpdatedAt(merchant.updatedAt());
	}

}
