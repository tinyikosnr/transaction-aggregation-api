package za.co.tinyiko.transactionaggregation.merchant.mapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.merchant.domain.Merchant;
import za.co.tinyiko.transactionaggregation.merchant.domain.MerchantId;
import za.co.tinyiko.transactionaggregation.merchant.persistence.MerchantEntity;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantMapperTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-31T08:15:00Z"), ZoneOffset.UTC);

	@Test
	void toDomainMapsAllFields() {
		MerchantEntity entity = new MerchantEntity();
		UUID id = UUID.randomUUID();
		entity.setId(id);
		entity.setNormalisedName("CHECKERS 104 CENTURION");
		entity.setDisplayName("Checkers #104 Centurion");
		entity.setCreatedAt(Instant.parse("2026-07-31T08:15:00Z"));
		entity.setUpdatedAt(Instant.parse("2026-07-31T09:00:00Z"));

		Merchant merchant = MerchantMapper.toDomain(entity);

		assertThat(merchant.id()).isEqualTo(new MerchantId(id));
		assertThat(merchant.normalisedName()).isEqualTo("CHECKERS 104 CENTURION");
		assertThat(merchant.displayName()).isEqualTo("Checkers #104 Centurion");
		assertThat(merchant.createdAt()).isEqualTo(Instant.parse("2026-07-31T08:15:00Z"));
		assertThat(merchant.updatedAt()).isEqualTo(Instant.parse("2026-07-31T09:00:00Z"));
	}

	@Test
	void applyToCopiesAllFieldsOntoEntity() {
		Merchant merchant = Merchant.register(MerchantId.generate(), "SPAR", "Spar", FIXED_CLOCK);
		MerchantEntity entity = new MerchantEntity();

		MerchantMapper.applyTo(entity, merchant);

		assertThat(entity.getId()).isEqualTo(merchant.id().value());
		assertThat(entity.getNormalisedName()).isEqualTo("SPAR");
		assertThat(entity.getDisplayName()).isEqualTo("Spar");
		assertThat(entity.getCreatedAt()).isEqualTo(merchant.createdAt());
		assertThat(entity.getUpdatedAt()).isEqualTo(merchant.updatedAt());
	}

	@Test
	void applyToDoesNotTouchVersion() {
		Merchant merchant = Merchant.register(MerchantId.generate(), "SPAR", "Spar", FIXED_CLOCK);
		MerchantEntity entity = new MerchantEntity();

		MerchantMapper.applyTo(entity, merchant);

		assertThat(entity.getVersion()).isNull();
	}

}
