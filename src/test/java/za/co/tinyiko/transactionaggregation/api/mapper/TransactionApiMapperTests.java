package za.co.tinyiko.transactionaggregation.api.mapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.api.dto.request.CreateTransactionRequest;
import za.co.tinyiko.transactionaggregation.api.dto.response.TransactionResponse;
import za.co.tinyiko.transactionaggregation.shared.logging.CorrelationId;
import za.co.tinyiko.transactionaggregation.transaction.application.CreateTransactionCommand;
import za.co.tinyiko.transactionaggregation.transaction.application.TransactionCreatedResult;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionApiMapperTests {

	private static final CorrelationId CORRELATION_ID = new CorrelationId("test-correlation-id");
	private static final String ACTOR = "jwt-subject-001";

	@Test
	void mapsEveryRequestFieldOntoTheCommandAlongsideTheCorrelationIdAndActor() {
		CreateTransactionRequest request = new CreateTransactionRequest(
				UUID.randomUUID(), "MOCK_BANK_A", "EXT-001", "Checkers", new BigDecimal("125.50"),
				"ZAR", "DEBIT", "groceries", Instant.parse("2026-08-06T08:00:00Z"));

		CreateTransactionCommand command = TransactionApiMapper.toCommand(request, CORRELATION_ID, ACTOR);

		assertThat(command.customerId()).isEqualTo(request.customerId());
		assertThat(command.sourceCode()).isEqualTo(request.sourceCode());
		assertThat(command.externalTransactionId()).isEqualTo(request.externalTransactionId());
		assertThat(command.merchantName()).isEqualTo(request.merchantName());
		assertThat(command.amount()).isEqualByComparingTo(request.amount());
		assertThat(command.currency()).isEqualTo(request.currency());
		assertThat(command.direction()).isEqualTo(request.direction());
		assertThat(command.description()).isEqualTo(request.description());
		assertThat(command.occurredAt()).isEqualTo(request.occurredAt());
		assertThat(command.correlationId()).isEqualTo(CORRELATION_ID);
		assertThat(command.actor()).isEqualTo(ACTOR);
	}

	@Test
	void mapsAResultWithAMerchantToAResponseWithAPopulatedMerchantInfo() {
		UUID id = UUID.randomUUID();
		UUID customerId = UUID.randomUUID();
		UUID merchantId = UUID.randomUUID();
		TransactionCreatedResult result = new TransactionCreatedResult(
				id, customerId, "MOCK_BANK_A", "EXT-001", merchantId, "Checkers",
				"GROCERIES", "Groceries", new BigDecimal("125.50"), "ZAR", "DEBIT", "desc", "PROCESSED",
				Instant.parse("2026-08-06T08:00:00Z"), Instant.parse("2026-08-06T08:00:01Z"), Instant.parse("2026-08-06T08:00:01Z"));

		TransactionResponse response = TransactionApiMapper.toResponse(result);

		assertThat(response.id()).isEqualTo(id);
		assertThat(response.customerId()).isEqualTo(customerId);
		assertThat(response.merchant()).isEqualTo(new TransactionResponse.MerchantInfo(merchantId, "Checkers"));
		assertThat(response.category()).isEqualTo(new TransactionResponse.CategoryInfo("GROCERIES", "Groceries"));
		assertThat(response.amount()).isEqualByComparingTo("125.50");
	}

	@Test
	void mapsAResultWithNoMerchantToANullMerchantInfo() {
		TransactionCreatedResult result = new TransactionCreatedResult(
				UUID.randomUUID(), UUID.randomUUID(), "MOCK_BANK_A", "EXT-001", null, null,
				"UNCATEGORISED", "Uncategorised", new BigDecimal("10.00"), "ZAR", "DEBIT", null, "PROCESSED",
				Instant.parse("2026-08-06T08:00:00Z"), Instant.parse("2026-08-06T08:00:01Z"), Instant.parse("2026-08-06T08:00:01Z"));

		TransactionResponse response = TransactionApiMapper.toResponse(result);

		assertThat(response.merchant()).isNull();
	}

}
