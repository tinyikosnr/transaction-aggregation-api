package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRule;
import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRuleId;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategory;
import za.co.tinyiko.transactionaggregation.categorisation.domain.TransactionCategoryId;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategorisationRuleRepositoryPort;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategorisationRuleRow;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategoryRepositoryPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategorisationRuleAdminServiceTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-10T08:15:00Z"), ZoneOffset.UTC);
	private static final UUID CATEGORY_ID = UUID.randomUUID();
	private static final UUID RULE_ID = UUID.randomUUID();

	@Mock
	private CategorisationRuleRepositoryPort categorisationRuleRepositoryPort;

	@Mock
	private CategoryRepositoryPort categoryRepositoryPort;

	private CategorisationRuleAdminService service() {
		return new CategorisationRuleAdminService(categorisationRuleRepositoryPort, categoryRepositoryPort, FIXED_CLOCK);
	}

	private static TransactionCategory anActiveCategory() {
		return TransactionCategory.register(new TransactionCategoryId(CATEGORY_ID), "GROCERIES", "Groceries", null, false, FIXED_CLOCK);
	}

	private static CategorisationRuleRow aSavedRow(boolean active, int priority, long version) {
		return new CategorisationRuleRow(RULE_ID, CATEGORY_ID, "MERCHANT", "CONTAINS", "SHELL", "DEBIT", priority,
				active, Instant.parse("2026-08-10T08:00:00Z"), Instant.parse("2026-08-10T08:15:00Z"), version);
	}

	private static CreateCategorisationRuleCommand aCreateCommand() {
		return new CreateCategorisationRuleCommand(CATEGORY_ID, "MERCHANT", "CONTAINS", "SHELL", "DEBIT", 30);
	}

	private static UpdateCategorisationRuleCommand anUpdateCommand() {
		return new UpdateCategorisationRuleCommand(CATEGORY_ID, "MERCHANT", "CONTAINS", "SHELL", "DEBIT", 30, true, 0L);
	}

	// --- create ---

	@Test
	void createPersistsAndReturnsTheSavedRuleView() {
		when(categoryRepositoryPort.findById(new TransactionCategoryId(CATEGORY_ID))).thenReturn(Optional.of(anActiveCategory()));
		when(categorisationRuleRepositoryPort.create(any())).thenReturn(aSavedRow(true, 30, 0L));

		CategorisationRuleView view = service().create(aCreateCommand());

		assertThat(view.id()).isEqualTo(RULE_ID);
		assertThat(view.version()).isEqualTo(0L);
		assertThat(view.active()).isTrue();
	}

	@Test
	void createThrowsCategoryNotFoundForAnUnknownCategoryAndNeverCallsCreate() {
		when(categoryRepositoryPort.findById(new TransactionCategoryId(CATEGORY_ID))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().create(aCreateCommand())).isInstanceOf(CategoryNotFoundException.class);
		verify(categorisationRuleRepositoryPort, never()).create(any());
	}

	@Test
	void createThrowsRuleValidationForAnUnknownMatchField() {
		when(categoryRepositoryPort.findById(new TransactionCategoryId(CATEGORY_ID))).thenReturn(Optional.of(anActiveCategory()));
		CreateCategorisationRuleCommand command = new CreateCategorisationRuleCommand(CATEGORY_ID, "NOT_A_FIELD", "CONTAINS", "SHELL", "DEBIT", 30);

		assertThatThrownBy(() -> service().create(command)).isInstanceOf(RuleValidationException.class);
	}

	@Test
	void createThrowsRuleValidationForAnUnknownOperator() {
		when(categoryRepositoryPort.findById(new TransactionCategoryId(CATEGORY_ID))).thenReturn(Optional.of(anActiveCategory()));
		CreateCategorisationRuleCommand command = new CreateCategorisationRuleCommand(CATEGORY_ID, "MERCHANT", "NOT_AN_OPERATOR", "SHELL", "DEBIT", 30);

		assertThatThrownBy(() -> service().create(command)).isInstanceOf(RuleValidationException.class);
	}

	@Test
	void createThrowsRuleValidationForAnUnknownDirection() {
		when(categoryRepositoryPort.findById(new TransactionCategoryId(CATEGORY_ID))).thenReturn(Optional.of(anActiveCategory()));
		CreateCategorisationRuleCommand command = new CreateCategorisationRuleCommand(CATEGORY_ID, "MERCHANT", "CONTAINS", "SHELL", "SIDEWAYS", 30);

		assertThatThrownBy(() -> service().create(command)).isInstanceOf(RuleValidationException.class);
	}

	@Test
	void createThrowsRuleValidationForAnInvalidRegexPattern() {
		when(categoryRepositoryPort.findById(new TransactionCategoryId(CATEGORY_ID))).thenReturn(Optional.of(anActiveCategory()));
		CreateCategorisationRuleCommand command = new CreateCategorisationRuleCommand(CATEGORY_ID, "DESCRIPTION", "REGEX", "[unterminated", "DEBIT", 30);

		assertThatThrownBy(() -> service().create(command)).isInstanceOf(RuleValidationException.class);
		verify(categorisationRuleRepositoryPort, never()).create(any());
	}

	@Test
	void createThrowsRuleValidationForANonPositivePriority() {
		when(categoryRepositoryPort.findById(new TransactionCategoryId(CATEGORY_ID))).thenReturn(Optional.of(anActiveCategory()));
		CreateCategorisationRuleCommand command = new CreateCategorisationRuleCommand(CATEGORY_ID, "MERCHANT", "CONTAINS", "SHELL", "DEBIT", 0);

		assertThatThrownBy(() -> service().create(command)).isInstanceOf(RuleValidationException.class);
	}

	@Test
	void createThrowsRuleValidationForABlankMatchValue() {
		when(categoryRepositoryPort.findById(new TransactionCategoryId(CATEGORY_ID))).thenReturn(Optional.of(anActiveCategory()));
		CreateCategorisationRuleCommand command = new CreateCategorisationRuleCommand(CATEGORY_ID, "MERCHANT", "CONTAINS", "   ", "DEBIT", 30);

		assertThatThrownBy(() -> service().create(command)).isInstanceOf(RuleValidationException.class);
	}

	@Test
	void createAlwaysRegistersTheRuleAsActive() {
		when(categoryRepositoryPort.findById(new TransactionCategoryId(CATEGORY_ID))).thenReturn(Optional.of(anActiveCategory()));
		when(categorisationRuleRepositoryPort.create(any())).thenReturn(aSavedRow(true, 30, 0L));

		ArgumentCaptor<CategorisationRule> captor = ArgumentCaptor.forClass(CategorisationRule.class);
		service().create(aCreateCommand());
		verify(categorisationRuleRepositoryPort).create(captor.capture());

		assertThat(captor.getValue().active()).isTrue();
		assertThat(captor.getValue().id()).isNotNull();
	}

	// --- update ---

	@Test
	void updatePersistsAndReturnsTheSavedRuleView() {
		when(categoryRepositoryPort.findById(new TransactionCategoryId(CATEGORY_ID))).thenReturn(Optional.of(anActiveCategory()));
		when(categorisationRuleRepositoryPort.update(eq(new CategorisationRuleId(RULE_ID)), any(), eq(0L)))
				.thenReturn(aSavedRow(false, 999, 1L));

		CategorisationRuleView view = service().update(RULE_ID, new UpdateCategorisationRuleCommand(
				CATEGORY_ID, "MERCHANT", "CONTAINS", "SHELL", "DEBIT", 999, false, 0L));

		assertThat(view.active()).isFalse();
		assertThat(view.priority()).isEqualTo(999);
		assertThat(view.version()).isEqualTo(1L);
	}

	@Test
	void updatePassesTheExpectedVersionThroughToThePort() {
		when(categoryRepositoryPort.findById(new TransactionCategoryId(CATEGORY_ID))).thenReturn(Optional.of(anActiveCategory()));
		when(categorisationRuleRepositoryPort.update(any(), any(), eq(5L))).thenReturn(aSavedRow(true, 30, 6L));

		service().update(RULE_ID, new UpdateCategorisationRuleCommand(CATEGORY_ID, "MERCHANT", "CONTAINS", "SHELL", "DEBIT", 30, true, 5L));

		verify(categorisationRuleRepositoryPort).update(eq(new CategorisationRuleId(RULE_ID)), any(), eq(5L));
	}

	@Test
	void updateThrowsCategoryNotFoundForAnUnknownCategoryAndNeverCallsUpdate() {
		when(categoryRepositoryPort.findById(new TransactionCategoryId(CATEGORY_ID))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().update(RULE_ID, anUpdateCommand())).isInstanceOf(CategoryNotFoundException.class);
		verify(categorisationRuleRepositoryPort, never()).update(any(), any(), anyLong());
	}

	@Test
	void updatePropagatesRuleNotFoundFromThePort() {
		when(categoryRepositoryPort.findById(new TransactionCategoryId(CATEGORY_ID))).thenReturn(Optional.of(anActiveCategory()));
		when(categorisationRuleRepositoryPort.update(any(), any(), eq(0L))).thenThrow(new RuleNotFoundException(RULE_ID));

		assertThatThrownBy(() -> service().update(RULE_ID, anUpdateCommand())).isInstanceOf(RuleNotFoundException.class);
	}

	@Test
	void updatePropagatesRuleConflictFromThePort() {
		when(categoryRepositoryPort.findById(new TransactionCategoryId(CATEGORY_ID))).thenReturn(Optional.of(anActiveCategory()));
		when(categorisationRuleRepositoryPort.update(any(), any(), eq(0L))).thenThrow(new RuleConflictException(RULE_ID, 0L, 1L));

		assertThatThrownBy(() -> service().update(RULE_ID, anUpdateCommand())).isInstanceOf(RuleConflictException.class);
	}

	@Test
	void updateThrowsRuleValidationForAnInvalidRegexPattern() {
		when(categoryRepositoryPort.findById(new TransactionCategoryId(CATEGORY_ID))).thenReturn(Optional.of(anActiveCategory()));
		UpdateCategorisationRuleCommand command = new UpdateCategorisationRuleCommand(
				CATEGORY_ID, "DESCRIPTION", "REGEX", "[unterminated", "DEBIT", 30, true, 0L);

		assertThatThrownBy(() -> service().update(RULE_ID, command)).isInstanceOf(RuleValidationException.class);
		verify(categorisationRuleRepositoryPort, never()).update(any(), any(), anyLong());
	}

}
