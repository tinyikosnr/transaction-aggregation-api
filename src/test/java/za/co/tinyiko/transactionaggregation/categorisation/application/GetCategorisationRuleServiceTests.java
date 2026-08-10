package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRuleId;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategorisationRuleRepositoryPort;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategorisationRuleRow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCategorisationRuleServiceTests {

	private static final UUID RULE_ID = UUID.randomUUID();

	@Mock
	private CategorisationRuleRepositoryPort categorisationRuleRepositoryPort;

	private GetCategorisationRuleService service() {
		return new GetCategorisationRuleService(categorisationRuleRepositoryPort);
	}

	@Test
	void returnsTheMatchingRuleIncludingVersion() {
		CategorisationRuleRow row = new CategorisationRuleRow(RULE_ID, UUID.randomUUID(), "MERCHANT", "CONTAINS",
				"SHELL", "DEBIT", 30, true, Instant.parse("2026-08-10T08:00:00Z"), Instant.parse("2026-08-10T08:00:00Z"), 2L);
		when(categorisationRuleRepositoryPort.findRowById(new CategorisationRuleId(RULE_ID))).thenReturn(Optional.of(row));

		CategorisationRuleView view = service().get(RULE_ID);

		assertThat(view.id()).isEqualTo(RULE_ID);
		assertThat(view.version()).isEqualTo(2L);
	}

	@Test
	void throwsRuleNotFoundWhenNoRuleMatches() {
		when(categorisationRuleRepositoryPort.findRowById(new CategorisationRuleId(RULE_ID))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().get(RULE_ID)).isInstanceOf(RuleNotFoundException.class);
	}

}
