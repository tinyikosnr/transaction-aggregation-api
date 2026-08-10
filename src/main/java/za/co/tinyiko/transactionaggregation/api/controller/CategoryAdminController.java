package za.co.tinyiko.transactionaggregation.api.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;

import za.co.tinyiko.transactionaggregation.api.dto.request.CreateCategorisationRuleRequest;
import za.co.tinyiko.transactionaggregation.api.dto.request.UpdateCategorisationRuleRequest;
import za.co.tinyiko.transactionaggregation.api.dto.response.CategorisationRuleResponse;
import za.co.tinyiko.transactionaggregation.api.dto.response.CategoryResponse;
import za.co.tinyiko.transactionaggregation.api.mapper.CategoryAdminApiMapper;
import za.co.tinyiko.transactionaggregation.categorisation.application.CategorisationRuleView;
import za.co.tinyiko.transactionaggregation.categorisation.application.CreateCategorisationRuleCommand;
import za.co.tinyiko.transactionaggregation.categorisation.application.CreateCategorisationRuleUseCase;
import za.co.tinyiko.transactionaggregation.categorisation.application.GetCategorisationRuleUseCase;
import za.co.tinyiko.transactionaggregation.categorisation.application.ListCategoriesUseCase;
import za.co.tinyiko.transactionaggregation.categorisation.application.ListCategorisationRulesUseCase;
import za.co.tinyiko.transactionaggregation.categorisation.application.UpdateCategorisationRuleCommand;
import za.co.tinyiko.transactionaggregation.categorisation.application.UpdateCategorisationRuleUseCase;

/**
 * Category and categorisation-rule administration (feature/category-admin, TDS 54's named
 * {@code CategoryAdminController}). The exact scope here - category reads only, full rule
 * create/update, no deletion anywhere - is an explicit project resolution of an incomplete
 * SAD/TDS contract, not something fully specified by the documentation; see CLAUDE.md for the
 * detailed reasoning. Every endpoint requires {@code CATEGORY_ADMIN}. Thin, like every other
 * controller: all validation, category-reference checking, and optimistic-lock handling lives in
 * {@code categorisation.application}, not here.
 */
@RestController
class CategoryAdminController {

	private final ListCategoriesUseCase listCategoriesUseCase;
	private final ListCategorisationRulesUseCase listCategorisationRulesUseCase;
	private final GetCategorisationRuleUseCase getCategorisationRuleUseCase;
	private final CreateCategorisationRuleUseCase createCategorisationRuleUseCase;
	private final UpdateCategorisationRuleUseCase updateCategorisationRuleUseCase;

	CategoryAdminController(
			ListCategoriesUseCase listCategoriesUseCase,
			ListCategorisationRulesUseCase listCategorisationRulesUseCase,
			GetCategorisationRuleUseCase getCategorisationRuleUseCase,
			CreateCategorisationRuleUseCase createCategorisationRuleUseCase,
			UpdateCategorisationRuleUseCase updateCategorisationRuleUseCase
	) {
		this.listCategoriesUseCase = listCategoriesUseCase;
		this.listCategorisationRulesUseCase = listCategorisationRulesUseCase;
		this.getCategorisationRuleUseCase = getCategorisationRuleUseCase;
		this.createCategorisationRuleUseCase = createCategorisationRuleUseCase;
		this.updateCategorisationRuleUseCase = updateCategorisationRuleUseCase;
	}

	@GetMapping("/api/v1/categories")
	@PreAuthorize("hasAuthority('CATEGORY_ADMIN')")
	List<CategoryResponse> listCategories() {
		return CategoryAdminApiMapper.toCategoryResponses(listCategoriesUseCase.list());
	}

	@GetMapping("/api/v1/categories/{id}")
	@PreAuthorize("hasAuthority('CATEGORY_ADMIN')")
	CategoryResponse getCategory(@PathVariable UUID id) {
		return CategoryAdminApiMapper.toResponse(listCategoriesUseCase.get(id));
	}

	@GetMapping("/api/v1/categorisation-rules")
	@PreAuthorize("hasAuthority('CATEGORY_ADMIN')")
	List<CategorisationRuleResponse> listRules() {
		return CategoryAdminApiMapper.toRuleResponses(listCategorisationRulesUseCase.list());
	}

	@GetMapping("/api/v1/categorisation-rules/{id}")
	@PreAuthorize("hasAuthority('CATEGORY_ADMIN')")
	CategorisationRuleResponse getRule(@PathVariable UUID id) {
		return CategoryAdminApiMapper.toResponse(getCategorisationRuleUseCase.get(id));
	}

	@PostMapping("/api/v1/categorisation-rules")
	@PreAuthorize("hasAuthority('CATEGORY_ADMIN')")
	ResponseEntity<CategorisationRuleResponse> createRule(@Valid @RequestBody CreateCategorisationRuleRequest request) {
		CreateCategorisationRuleCommand command = CategoryAdminApiMapper.toCommand(request);
		CategorisationRuleView created = createCategorisationRuleUseCase.create(command);
		CategorisationRuleResponse response = CategoryAdminApiMapper.toResponse(created);

		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(location).body(response);
	}

	@PutMapping("/api/v1/categorisation-rules/{id}")
	@PreAuthorize("hasAuthority('CATEGORY_ADMIN')")
	CategorisationRuleResponse updateRule(@PathVariable UUID id, @Valid @RequestBody UpdateCategorisationRuleRequest request) {
		UpdateCategorisationRuleCommand command = CategoryAdminApiMapper.toCommand(request);
		CategorisationRuleView updated = updateCategorisationRuleUseCase.update(id, command);
		return CategoryAdminApiMapper.toResponse(updated);
	}

}
