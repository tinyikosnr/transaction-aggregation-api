package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.util.UUID;

import org.springframework.stereotype.Service;

import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRuleId;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategorisationRuleRepositoryPort;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategorisationRuleRow;

@Service
class GetCategorisationRuleService implements GetCategorisationRuleUseCase {

	private final CategorisationRuleRepositoryPort categorisationRuleRepositoryPort;

	GetCategorisationRuleService(CategorisationRuleRepositoryPort categorisationRuleRepositoryPort) {
		this.categorisationRuleRepositoryPort = categorisationRuleRepositoryPort;
	}

	@Override
	public CategorisationRuleView get(UUID ruleId) {
		CategorisationRuleRow row = categorisationRuleRepositoryPort.findRowById(new CategorisationRuleId(ruleId))
				.orElseThrow(() -> new RuleNotFoundException(ruleId));
		return ListCategorisationRulesService.toView(row);
	}

}
