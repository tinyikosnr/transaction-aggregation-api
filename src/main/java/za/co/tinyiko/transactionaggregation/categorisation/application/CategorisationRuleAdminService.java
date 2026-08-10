package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.time.Clock;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRule;
import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRuleId;
import za.co.tinyiko.transactionaggregation.categorisation.domain.Direction;
import za.co.tinyiko.transactionaggregation.categorisation.domain.MatchField;
import za.co.tinyiko.transactionaggregation.categorisation.domain.MatchOperator;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategoryId;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategorisationRuleRepositoryPort;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategorisationRuleRow;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategoryRepositoryPort;

/**
 * Implements both {@link CreateCategorisationRuleUseCase} and {@link UpdateCategorisationRuleUseCase} -
 * one small service, not two, since they share the same field-level validation, the same
 * category-reference check, and the same repository. {@link ListCategorisationRulesUseCase}/
 * {@link GetCategorisationRuleUseCase} stay separate services (different failure modes, pure
 * reads) - see those interfaces' own Javadoc.
 *
 * <p>Each method is a single-aggregate write wrapped in its own {@code @Transactional} boundary,
 * matching every other write use case in this codebase. {@code update} does not itself contain
 * the optimistic-lock race-window logic - that lives entirely in {@code
 * JpaCategorisationRuleRepositoryAdapter#update}, which loads, compares, mutates and flushes one
 * single managed entity in one call (see that adapter's own Javadoc). This service only ever
 * makes one port call for the actual write; it never reads the row separately beforehand and
 * never re-reads it afterward, so nothing here creates the possibility of a second, later read
 * observing a fresher version than the one being compared.
 */
@Service
class CategorisationRuleAdminService implements CreateCategorisationRuleUseCase, UpdateCategorisationRuleUseCase {

	private final CategorisationRuleRepositoryPort categorisationRuleRepositoryPort;
	private final CategoryRepositoryPort categoryRepositoryPort;
	private final Clock clock;

	CategorisationRuleAdminService(
			CategorisationRuleRepositoryPort categorisationRuleRepositoryPort,
			CategoryRepositoryPort categoryRepositoryPort,
			Clock clock
	) {
		this.categorisationRuleRepositoryPort = categorisationRuleRepositoryPort;
		this.categoryRepositoryPort = categoryRepositoryPort;
		this.clock = clock;
	}

	@Override
	@Transactional
	public CategorisationRuleView create(CreateCategorisationRuleCommand command) {
		requireCategoryExists(command.categoryId());
		MatchField matchField = parseMatchField(command.matchField());
		MatchOperator operator = parseOperator(command.operator());
		String matchValue = requireValidPattern(command.matchValue(), operator);
		Direction direction = parseDirection(command.direction());

		CategorisationRule rule;
		try {
			rule = CategorisationRule.register(
					CategorisationRuleId.generate(),
					new TransactionCategoryId(command.categoryId()),
					matchField, operator, matchValue, direction, command.priority(), clock);
		} catch (IllegalArgumentException e) {
			throw new RuleValidationException(e.getMessage(), e);
		}

		CategorisationRuleRow saved = categorisationRuleRepositoryPort.create(rule);
		return ListCategorisationRulesService.toView(saved);
	}

	@Override
	@Transactional
	public CategorisationRuleView update(UUID ruleId, UpdateCategorisationRuleCommand command) {
		requireCategoryExists(command.categoryId());
		MatchField matchField = parseMatchField(command.matchField());
		MatchOperator operator = parseOperator(command.operator());
		String matchValue = requireValidPattern(command.matchValue(), operator);
		Direction direction = parseDirection(command.direction());

		CategorisationRuleId id = new CategorisationRuleId(ruleId);
		CategorisationRule updatedFields;
		try {
			updatedFields = CategorisationRule.update(
					id, new TransactionCategoryId(command.categoryId()),
					matchField, operator, matchValue, direction, command.priority(), command.active(), clock);
		} catch (IllegalArgumentException e) {
			throw new RuleValidationException(e.getMessage(), e);
		}

		CategorisationRuleRow saved = categorisationRuleRepositoryPort.update(id, updatedFields, command.expectedVersion());
		return ListCategorisationRulesService.toView(saved);
	}

	private void requireCategoryExists(UUID categoryId) {
		categoryRepositoryPort.findById(new TransactionCategoryId(categoryId))
				.orElseThrow(() -> new CategoryNotFoundException(categoryId));
	}

	private static MatchField parseMatchField(String value) {
		try {
			return MatchField.valueOf(value);
		} catch (IllegalArgumentException e) {
			throw new RuleValidationException("matchField must be one of " + Arrays.toString(MatchField.values()), e);
		}
	}

	private static MatchOperator parseOperator(String value) {
		try {
			return MatchOperator.valueOf(value);
		} catch (IllegalArgumentException e) {
			throw new RuleValidationException("operator must be one of " + Arrays.toString(MatchOperator.values()), e);
		}
	}

	private static Direction parseDirection(String value) {
		try {
			return Direction.valueOf(value);
		} catch (IllegalArgumentException e) {
			throw new RuleValidationException("direction must be one of " + Arrays.toString(Direction.values()), e);
		}
	}

	/**
	 * Rejects an invalid {@code REGEX} pattern eagerly, at write time - {@link
	 * CategorisationRule#matches} only compiles a {@code REGEX} rule's pattern lazily, the first
	 * time it is evaluated against a real transaction, so without this check a broken pattern
	 * would not surface until it broke runtime categorisation for an actual customer transaction.
	 */
	private static String requireValidPattern(String matchValue, MatchOperator operator) {
		if (operator == MatchOperator.REGEX && matchValue != null) {
			try {
				Pattern.compile(matchValue);
			} catch (PatternSyntaxException e) {
				throw new RuleValidationException("matchValue is not a valid regular expression", e);
			}
		}
		return matchValue;
	}

}
