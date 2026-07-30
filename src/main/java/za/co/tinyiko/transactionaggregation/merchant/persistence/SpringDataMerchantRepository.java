package za.co.tinyiko.transactionaggregation.merchant.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataMerchantRepository extends JpaRepository<MerchantEntity, UUID> {

	Optional<MerchantEntity> findByNormalisedName(String normalisedName);

}
