package za.co.tinyiko.transactionaggregation.categorisation.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRule;
import za.co.tinyiko.transactionaggregation.categorisation.mapper.CategorisationRuleMapper;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategorisationRuleRepositoryPort;

@Component
class JpaCategorisationRuleRepositoryAdapter implements CategorisationRuleRepositoryPort {

	private final SpringDataCategorisationRuleRepository springDataCategorisationRuleRepository;

	JpaCategorisationRuleRepositoryAdapter(SpringDataCategorisationRuleRepository springDataCategorisationRuleRepository) {
		this.springDataCategorisationRuleRepository = springDataCategorisationRuleRepository;
	}

	@Override
	public List<CategorisationRule> findAllActive() {
		return springDataCategorisationRuleRepository.findAllByActiveTrue().stream()
				.map(CategorisationRuleMapper::toDomain)
				.toList();
	}

}
