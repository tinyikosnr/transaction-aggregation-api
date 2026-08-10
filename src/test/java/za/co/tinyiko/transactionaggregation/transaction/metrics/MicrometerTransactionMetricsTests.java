package za.co.tinyiko.transactionaggregation.transaction.metrics;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import za.co.tinyiko.transactionaggregation.transaction.port.RejectionReason;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionMetricsPort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No Spring context needed - {@link SimpleMeterRegistry} is a plain, in-memory Micrometer
 * registry, exactly like the real {@code PrometheusMeterRegistry} would be for these assertions.
 * Proves both the counting/outcome semantics and the cardinality-safety policy (SAD 38.3):
 * exactly the {@code reason} tag on {@code transactions.rejected}, bounded to the three
 * {@link RejectionReason} values, and never a UUID/correlation/actor/exception-message tag
 * anywhere.
 */
class MicrometerTransactionMetricsTests {

	private static final Set<String> FORBIDDEN_TAG_KEYS = Set.of(
			"transactionId", "customerId", "correlationId", "externalTransactionId", "merchantName", "actor");

	private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
	private final TransactionMetricsPort metrics = new MicrometerTransactionMetrics(registry);

	@Test
	void transactionReceivedIncrementsAnUntaggedCounter() {
		metrics.transactionReceived();

		assertThat(registry.get("transactions.received").counter().count()).isEqualTo(1.0);
	}

	@Test
	void transactionProcessedIncrementsAnUntaggedCounter() {
		metrics.transactionProcessed();

		assertThat(registry.get("transactions.processed").counter().count()).isEqualTo(1.0);
	}

	@Test
	void transactionDuplicateRejectedIncrementsItsOwnCounterNotTransactionsRejected() {
		metrics.transactionDuplicateRejected();

		assertThat(registry.get("transactions.duplicates").counter().count()).isEqualTo(1.0);
		// The three transactions.rejected/reason counters are pre-registered at construction time
		// (a bounded, fixed set - see MicrometerTransactionMetrics), so they exist but must still
		// read zero: transactionDuplicateRejected() must never touch any of them.
		assertThat(registry.find("transactions.rejected").counters()).allSatisfy(counter -> assertThat(counter.count()).isZero());
	}

	@Test
	void categorisationFallbackIncrementsAnUntaggedCounter() {
		metrics.categorisationFallback();

		assertThat(registry.get("transactions.categorisation.fallback").counter().count()).isEqualTo(1.0);
	}

	@Test
	void transactionRejectedTagsExactlyTheGivenReasonAndOnlyThatCounter() {
		metrics.transactionRejected(RejectionReason.SOURCE_NOT_FOUND);

		assertThat(registry.get("transactions.rejected").tag("reason", "SOURCE_NOT_FOUND").counter().count()).isEqualTo(1.0);
		assertThat(registry.get("transactions.rejected").tag("reason", "VALIDATION_FAILED").counter().count()).isZero();
		assertThat(registry.get("transactions.rejected").tag("reason", "CUSTOMER_NOT_FOUND").counter().count()).isZero();
	}

	@Test
	void transactionRejectedReasonTagIsBoundedToTheThreeClosedEnumValues() {
		for (RejectionReason reason : RejectionReason.values()) {
			metrics.transactionRejected(reason);
		}

		List<String> observedReasonValues = registry.find("transactions.rejected").meters().stream()
				.flatMap(meter -> meter.getId().getTags().stream())
				.filter(tag -> tag.getKey().equals("reason"))
				.map(Tag::getValue)
				.distinct()
				.toList();

		assertThat(observedReasonValues).containsExactlyInAnyOrder("VALIDATION_FAILED", "SOURCE_NOT_FOUND", "CUSTOMER_NOT_FOUND");
	}

	@Test
	void processingTimerRecordsExactlyOneObservationPerStopRegardlessOfOutcome() {
		TransactionMetricsPort.ProcessingTimer timer = metrics.startProcessingTimer();
		timer.stop();

		assertThat(registry.get("transactions.processing.duration").timer().count()).isEqualTo(1L);
	}

	@Test
	void noMeterAnywhereCarriesAForbiddenHighCardinalityTagKey() {
		metrics.transactionReceived();
		metrics.transactionProcessed();
		metrics.transactionDuplicateRejected();
		metrics.categorisationFallback();
		for (RejectionReason reason : RejectionReason.values()) {
			metrics.transactionRejected(reason);
		}
		metrics.startProcessingTimer().stop();

		for (Meter meter : registry.getMeters()) {
			for (Tag tag : meter.getId().getTags()) {
				assertThat(FORBIDDEN_TAG_KEYS).as("meter %s must not carry tag key %s", meter.getId().getName(), tag.getKey())
						.doesNotContain(tag.getKey());
			}
		}
	}

	@Test
	void noMeterNameCarriesTheTotalOrUnitSuffixInJava() {
		metrics.transactionReceived();
		metrics.transactionProcessed();
		metrics.transactionDuplicateRejected();
		metrics.categorisationFallback();
		metrics.transactionRejected(RejectionReason.VALIDATION_FAILED);
		metrics.startProcessingTimer().stop();

		List<String> meterNames = registry.getMeters().stream().map(meter -> meter.getId().getName()).toList();

		assertThat(meterNames).noneMatch(name -> name.endsWith(".total") || name.endsWith(".seconds"));
		assertThat(meterNames).contains(
				"transactions.received",
				"transactions.processed",
				"transactions.duplicates",
				"transactions.categorisation.fallback",
				"transactions.rejected",
				"transactions.processing.duration");
	}

}
