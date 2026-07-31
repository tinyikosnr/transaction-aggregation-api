package za.co.tinyiko.transactionaggregation.categorisation.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCategorisationRuleRepository extends JpaRepository<CategorisationRuleEntity, UUID> {

	List<CategorisationRuleEntity> findAllByActiveTrue();

}
