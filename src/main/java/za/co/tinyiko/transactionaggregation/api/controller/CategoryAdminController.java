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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

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
 * SAD/TDS contract, not something fully specified by the documentation. Every endpoint
 * requires {@code CATEGORY_ADMIN}. Thin, like every other controller: all validation,
 * category-reference checking, and optimistic-lock handling lives in
 * {@code categorisation.application}, not here.
 */
@RestController
@Tag(name = "Category Administration", description = "Read categories and administer categorisation rules (requires CATEGORY_ADMIN). No category write path and no rule deletion path exist - see each operation's description.")
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
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "List all categories", description = "Returns every seeded transaction category, active and inactive. Read-only - no category write path exists. Requires the CATEGORY_ADMIN authority.")
	@ApiResponse(responseCode = "200", description = "Categories returned.")
	@ApiResponse(responseCode = "401", description = "AUTHENTICATION_REQUIRED - missing or invalid bearer token.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "403", description = "ACCESS_DENIED - authenticated but missing CATEGORY_ADMIN.", content = @Content(mediaType = "application/problem+json"))
	List<CategoryResponse> listCategories() {
		return CategoryAdminApiMapper.toCategoryResponses(listCategoriesUseCase.list());
	}

	@GetMapping("/api/v1/categories/{id}")
	@PreAuthorize("hasAuthority('CATEGORY_ADMIN')")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Get a single category", description = "Requires the CATEGORY_ADMIN authority.")
	@ApiResponse(responseCode = "200", description = "Category found.")
	@ApiResponse(responseCode = "401", description = "AUTHENTICATION_REQUIRED - missing or invalid bearer token.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "403", description = "ACCESS_DENIED - authenticated but missing CATEGORY_ADMIN.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "404", description = "CATEGORY_NOT_FOUND - no category exists with the given id.", content = @Content(mediaType = "application/problem+json"))
	CategoryResponse getCategory(
			@Parameter(description = "Category identifier.") @PathVariable UUID id
	) {
		return CategoryAdminApiMapper.toResponse(listCategoriesUseCase.get(id));
	}

	@GetMapping("/api/v1/categorisation-rules")
	@PreAuthorize("hasAuthority('CATEGORY_ADMIN')")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "List all categorisation rules", description = "Returns every rule, active and inactive, in no particular guaranteed order (priority is not required to be unique or dense). Requires the CATEGORY_ADMIN authority.")
	@ApiResponse(responseCode = "200", description = "Rules returned.")
	@ApiResponse(responseCode = "401", description = "AUTHENTICATION_REQUIRED - missing or invalid bearer token.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "403", description = "ACCESS_DENIED - authenticated but missing CATEGORY_ADMIN.", content = @Content(mediaType = "application/problem+json"))
	List<CategorisationRuleResponse> listRules() {
		return CategoryAdminApiMapper.toRuleResponses(listCategorisationRulesUseCase.list());
	}

	@GetMapping("/api/v1/categorisation-rules/{id}")
	@PreAuthorize("hasAuthority('CATEGORY_ADMIN')")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Get a single categorisation rule", description = "Requires the CATEGORY_ADMIN authority.")
	@ApiResponse(responseCode = "200", description = "Rule found.")
	@ApiResponse(responseCode = "401", description = "AUTHENTICATION_REQUIRED - missing or invalid bearer token.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "403", description = "ACCESS_DENIED - authenticated but missing CATEGORY_ADMIN.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "404", description = "RULE_NOT_FOUND - no rule exists with the given id.", content = @Content(mediaType = "application/problem+json"))
	CategorisationRuleResponse getRule(
			@Parameter(description = "Rule identifier.") @PathVariable UUID id
	) {
		return CategoryAdminApiMapper.toResponse(getCategorisationRuleUseCase.get(id));
	}

	@PostMapping("/api/v1/categorisation-rules")
	@PreAuthorize("hasAuthority('CATEGORY_ADMIN')")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Create a categorisation rule", description = "Creates a new rule, active by default. priority is not required to be unique or dense; duplicate priorities across rules are permitted. Requires the CATEGORY_ADMIN authority.")
	@ApiResponse(responseCode = "201", description = "Rule created; Location header points to GET /api/v1/categorisation-rules/{id}.")
	@ApiResponse(responseCode = "400", description = "REQUEST_VALIDATION_FAILED - request failed structural or business validation (e.g. unknown matchField/operator/direction value).", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "401", description = "AUTHENTICATION_REQUIRED - missing or invalid bearer token.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "403", description = "ACCESS_DENIED - authenticated but missing CATEGORY_ADMIN.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "404", description = "CATEGORY_NOT_FOUND - categoryId does not resolve to an existing category.", content = @Content(mediaType = "application/problem+json"))
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
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Replace a categorisation rule", description = """
			Full-replacement PUT semantics: every field, including categoryId (mutable) and active, must be \
			supplied. Requires expectedVersion to match the rule's current optimistic-locking version - a \
			stale value is rejected rather than silently overwritten. There is no rule deletion endpoint; use \
			active=false to retire a rule instead. Requires the CATEGORY_ADMIN authority.""")
	@ApiResponse(responseCode = "200", description = "Rule replaced; response reflects the new version.")
	@ApiResponse(responseCode = "400", description = "REQUEST_VALIDATION_FAILED - request failed structural or business validation.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "401", description = "AUTHENTICATION_REQUIRED - missing or invalid bearer token.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "403", description = "ACCESS_DENIED - authenticated but missing CATEGORY_ADMIN.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "404", description = "RULE_NOT_FOUND or CATEGORY_NOT_FOUND - the rule id, or the request's categoryId, does not resolve.", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "409", description = "OPTIMISTIC_LOCK_CONFLICT - expectedVersion no longer matches the rule's current version.", content = @Content(mediaType = "application/problem+json"))
	CategorisationRuleResponse updateRule(
			@Parameter(description = "Rule identifier.") @PathVariable UUID id,
			@Valid @RequestBody UpdateCategorisationRuleRequest request
	) {
		UpdateCategorisationRuleCommand command = CategoryAdminApiMapper.toCommand(request);
		CategorisationRuleView updated = updateCategorisationRuleUseCase.update(id, command);
		return CategoryAdminApiMapper.toResponse(updated);
	}

}
