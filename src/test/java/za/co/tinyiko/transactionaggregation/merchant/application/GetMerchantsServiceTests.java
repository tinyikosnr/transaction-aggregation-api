package za.co.tinyiko.transactionaggregation.merchant.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.tinyiko.transactionaggregation.merchant.domain.Merchant;
import za.co.tinyiko.transactionaggregation.merchant.domain.MerchantId;
import za.co.tinyiko.transactionaggregation.merchant.port.MerchantRepositoryPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMerchantsServiceTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-06T08:15:00Z"), ZoneOffset.UTC);
	private static final UUID MERCHANT_ID = UUID.randomUUID();

	@Mock
	private MerchantRepositoryPort merchantRepositoryPort;

	private GetMerchantsService service() {
		return new GetMerchantsService(merchantRepositoryPort);
	}

	private static Merchant aMerchant() {
		return Merchant.register(new MerchantId(MERCHANT_ID), "CHECKERS", "Checkers", FIXED_CLOCK);
	}

	@Test
	void getReturnsTheMatchingMerchantView() {
		when(merchantRepositoryPort.findByIds(List.of(new MerchantId(MERCHANT_ID)))).thenReturn(List.of(aMerchant()));

		Optional<MerchantView> view = service().get(MERCHANT_ID);

		assertThat(view).contains(new MerchantView(MERCHANT_ID, "Checkers"));
	}

	@Test
	void getReturnsEmptyWhenNoMerchantMatches() {
		when(merchantRepositoryPort.findByIds(List.of(new MerchantId(MERCHANT_ID)))).thenReturn(List.of());

		assertThat(service().get(MERCHANT_ID)).isEmpty();
	}

	@Test
	void findByIdsReturnsAViewPerMatchedMerchant() {
		when(merchantRepositoryPort.findByIds(List.of(new MerchantId(MERCHANT_ID)))).thenReturn(List.of(aMerchant()));

		List<MerchantView> views = service().findByIds(List.of(MERCHANT_ID));

		assertThat(views).containsExactly(new MerchantView(MERCHANT_ID, "Checkers"));
	}

}
