package za.co.tinyiko.transactionaggregation.categorisation.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.tinyiko.transactionaggregation.categorisation.port.CategorisationRuleRepositoryPort;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategorisationRuleRow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListCategorisationRulesServiceTests {

	@Mock
	private CategorisationRuleRepositoryPort categorisationRuleRepositoryPort;

	private ListCategorisationRulesService service() {
		return new ListCategorisationRulesService(categorisationRuleRepositoryPort);
	}

	private static CategorisationRuleRow aRow() {
		return new CategorisationRuleRow(UUID.randomUUID(), UUID.randomUUID(), "MERCHANT", "CONTAINS", "SHELL",
				"DEBIT", 30, true, Instant.parse("2026-08-10T08:00:00Z"), Instant.parse("2026-08-10T08:00:00Z"), 0L);
	}

	@Test
	void listReturnsAViewPerRowIncludingVersion() {
		CategorisationRuleRow row = aRow();
		when(categorisationRuleRepositoryPort.findAll()).thenReturn(List.of(row));

		List<CategorisationRuleView> views = service().list();

		assertThat(views).hasSize(1);
		assertThat(views.get(0).id()).isEqualTo(row.id());
		assertThat(views.get(0).version()).isEqualTo(0L);
		assertThat(views.get(0).matchValue()).isEqualTo("SHELL");
	}

	@Test
	void listReturnsEmptyWhenNoRulesExist() {
		when(categorisationRuleRepositoryPort.findAll()).thenReturn(List.of());

		assertThat(service().list()).isEmpty();
	}

}
