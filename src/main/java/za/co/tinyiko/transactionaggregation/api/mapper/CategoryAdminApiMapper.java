package za.co.tinyiko.transactionaggregation.api.mapper;

import java.util.List;

import za.co.tinyiko.transactionaggregation.api.dto.request.CreateCategorisationRuleRequest;
import za.co.tinyiko.transactionaggregation.api.dto.request.UpdateCategorisationRuleRequest;
import za.co.tinyiko.transactionaggregation.api.dto.response.CategorisationRuleResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.CategoryResponse;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategoryAdminView;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategorisationRuleView;
import za.co.tinyiko.transactionaggregation.categorisation.application.CreateCategorisationRuleCommand;
import za.co.tinyiko.transactionaggregation.categorisation.application.UpdateCategorisationRuleCommand;

/**
 * Pure structural mapping between {@code api.dto} shapes and {@code categorisation.application}
 * contracts (feature/category-admin) - no business logic, matching every other mapper in this
 * codebase.
 */
public final class CategoryAdminApiMapper {

	private CategoryAdminApiMapper() {
	}

	public static CategoryResponse toResponse(CategoryAdminView view) {
		return new CategoryResponse(
				view.id(), view.code(), view.name(), view.description(),
				view.fallback(), view.active(), view.createdAt(), view.updatedAt());
	}

	public static List<CategoryResponse> toCategoryResponses(List<CategoryAdminView> views) {
		return views.stream().map(CategoryAdminApiMapper::toResponse).toList();
	}

	public static CategorisationRuleResponse toResponse(CategorisationRuleView view) {
		return new CategorisationRuleResponse(
				view.id(), view.categoryId(), view.matchField(), view.operator(), view.matchValue(),
				view.direction(), view.priority(), view.active(), view.version(), view.createdAt(), view.updatedAt());
	}

	public static List<CategorisationRuleResponse> toRuleResponses(List<CategorisationRuleView> views) {
		return views.stream().map(CategoryAdminApiMapper::toResponse).toList();
	}

	public static CreateCategorisationRuleCommand toCommand(CreateCategorisationRuleRequest request) {
		return new CreateCategorisationRuleCommand(
				request.categoryId(), request.matchField(), request.operator(),
				request.matchValue(), request.direction(), request.priority());
	}

	public static UpdateCategorisationRuleCommand toCommand(UpdateCategorisationRuleRequest request) {
		return new UpdateCategorisationRuleCommand(
				request.categoryId(), request.matchField(), request.operator(), request.matchValue(),
				request.direction(), request.priority(), request.active(), request.expectedVersion());
	}

}
