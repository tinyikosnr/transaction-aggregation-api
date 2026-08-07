package za.co.tinyiko.transactionaggregation.merchant.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.tinyiko.transactionaggregation.merchant.domain.Merchant;
import za.co.tinyiko.transactionaggregation.merchant.domain.MerchantId;
import za.co.tinyiko.transactionaggregation.merchant.port.MerchantRepositoryPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantResolutionServiceTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-31T08:15:00Z"), ZoneOffset.UTC);

	@Mock
	private MerchantRepositoryPort merchantRepositoryPort;

	@Test
	void returnsExistingMerchantWhenNormalisedNameMatches() {
		Merchant existing = Merchant.register(MerchantId.generate(), "CHECKERS 104 CENTURION",
				"Checkers #104 Centurion", FIXED_CLOCK);
		when(merchantRepositoryPort.findByNormalisedName("CHECKERS 104 CENTURION"))
				.thenReturn(Optional.of(existing));

		MerchantResolutionService service = new MerchantResolutionService(merchantRepositoryPort, FIXED_CLOCK);

		MerchantResolutionResult resolved = service.resolve("  Checkers #104 Centurion ");

		assertThat(resolved.merchantId()).isEqualTo(existing.id().value());
		assertThat(resolved.displayName()).isEqualTo(existing.displayName());
		verify(merchantRepositoryPort, never()).save(any());
	}

	@Test
	void looksUpByTheNormalisedFormNotTheRawInput() {
		when(merchantRepositoryPort.findByNormalisedName("SPAR")).thenReturn(Optional.empty());
		when(merchantRepositoryPort.save(any(Merchant.class))).thenAnswer(invocation -> invocation.getArgument(0));

		MerchantResolutionService service = new MerchantResolutionService(merchantRepositoryPort, FIXED_CLOCK);
		service.resolve("spar");

		verify(merchantRepositoryPort).findByNormalisedName(eq("SPAR"));
	}

	@Test
	void createsAndSavesANewMerchantWhenNotFound() {
		when(merchantRepositoryPort.findByNormalisedName("WOOLWORTHS")).thenReturn(Optional.empty());
		when(merchantRepositoryPort.save(any(Merchant.class))).thenAnswer(invocation -> invocation.getArgument(0));

		MerchantResolutionService service = new MerchantResolutionService(merchantRepositoryPort, FIXED_CLOCK);
		MerchantResolutionResult resolved = service.resolve(" Woolworths ");

		assertThat(resolved.displayName()).isEqualTo("Woolworths");

		ArgumentCaptor<Merchant> captor = ArgumentCaptor.forClass(Merchant.class);
		verify(merchantRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().normalisedName()).isEqualTo("WOOLWORTHS");
		assertThat(resolved.merchantId()).isEqualTo(captor.getValue().id().value());
	}

	@Test
	void recoversFromADuplicateCreatedConcurrently() {
		Merchant concurrentlyCreated = Merchant.register(MerchantId.generate(), "SHELL", "Shell", FIXED_CLOCK);
		when(merchantRepositoryPort.findByNormalisedName("SHELL"))
				.thenReturn(Optional.empty())
				.thenReturn(Optional.of(concurrentlyCreated));
		when(merchantRepositoryPort.save(any(Merchant.class)))
				.thenThrow(new DuplicateMerchantException("SHELL", new RuntimeException("constraint violation")));

		MerchantResolutionService service = new MerchantResolutionService(merchantRepositoryPort, FIXED_CLOCK);
		MerchantResolutionResult resolved = service.resolve("Shell");

		assertThat(resolved.merchantId()).isEqualTo(concurrentlyCreated.id().value());
		assertThat(resolved.displayName()).isEqualTo(concurrentlyCreated.displayName());
		verify(merchantRepositoryPort, times(2)).findByNormalisedName("SHELL");
	}

}
