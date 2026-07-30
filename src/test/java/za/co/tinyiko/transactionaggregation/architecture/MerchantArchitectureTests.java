package za.co.tinyiko.transactionaggregation.architecture;

import java.util.List;

import org.junit.jupiter.api.Test;

import za.co.tinyiko.transactionaggregation.merchant.application.DuplicateMerchantException;
import za.co.tinyiko.transactionaggregation.merchant.application.MerchantResolutionPort;
import za.co.tinyiko.transactionaggregation.merchant.domain.Merchant;
import za.co.tinyiko.transactionaggregation.merchant.domain.MerchantId;
import za.co.tinyiko.transactionaggregation.merchant.domain.MerchantNormaliser;

/**
 * Same two rules as {@link CustomerArchitectureTests}, applied to {@code merchant}: domain
 * framework-independence, and application not depending on persistence. Reusing
 * {@link FrameworkIndependenceAssertions} rather than introducing ArchUnit — proportionate at
 * this scale (two rules, five known classes), same reasoning as before.
 */
class MerchantArchitectureTests {

	private static final List<Class<?>> DOMAIN_TYPES = List.of(
			Merchant.class,
			MerchantId.class,
			MerchantNormaliser.class
	);

	private static final List<Class<?>> APPLICATION_TYPES = List.of(
			MerchantResolutionPort.class,
			DuplicateMerchantException.class
	);

	@Test
	void domainDoesNotReferenceFrameworkTypes() {
		DOMAIN_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(
				type, "org.springframework", "jakarta.persistence"));
	}

	@Test
	void applicationDoesNotReferencePersistence() {
		APPLICATION_TYPES.forEach(type -> FrameworkIndependenceAssertions.assertNoForbiddenReference(
				type, "za.co.tinyiko.transactionaggregation.merchant.persistence"));
	}

}
