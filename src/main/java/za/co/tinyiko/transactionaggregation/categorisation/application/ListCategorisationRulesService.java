package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.util.List;

import org.springframework.stereotype.Service;

import za.co.tinyiko.transactionaggregation.categorisation.port.CategorisationRuleRepositoryPort;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategorisationRuleRow;

@Service
class ListCategorisationRulesService implements ListCategorisationRulesUseCase {

	private final CategorisationRuleRepositoryPort categorisationRuleRepositoryPort;

	ListCategorisationRulesService(CategorisationRuleRepositoryPort categorisationRuleRepositoryPort) {
		this.categorisationRuleRepositoryPort = categorisationRuleRepositoryPort;
	}

	@Override
	public List<CategorisationRuleView> list() {
		return categorisationRuleRepositoryPort.findAll().stream()
				.map(ListCategorisationRulesService::toView)
				.toList();
	}

	static CategorisationRuleView toView(CategorisationRuleRow row) {
		return new CategorisationRuleView(
				row.id(),
				row.categoryId(),
				row.matchField(),
				row.operator(),
				row.matchValue(),
				row.direction(),
				row.priority(),
				row.active(),
				row.version(),
				row.createdAt(),
				row.updatedAt());
	}

}
