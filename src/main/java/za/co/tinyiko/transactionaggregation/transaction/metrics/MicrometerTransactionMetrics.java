package za.co.tinyiko.transactionaggregation.transaction.metrics;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import za.co.tinyiko.transactionaggregation.transaction.port.RejectionReason;
import za.co.tinyiko.transactionaggregation.transaction.port.TransactionMetricsPort;

/**
 * The sole Micrometer-backed implementation of {@link TransactionMetricsPort}
 * (feature/observability) - the only class in this codebase permitted to import
 * {@code io.micrometer.*} for this concern (enforced by
 * {@code architecture.TransactionArchitectureTests}). Package-private and {@code @Component}: the
 * same "no port needs to be a public type, only its interface matters to callers" convention this
 * codebase already applies to every other adapter (e.g. {@code JpaTransactionRepositoryAdapter}).
 *
 * <p>Meter names deliberately carry no {@code .total}/unit suffix in Java - Micrometer's
 * {@code PrometheusMeterRegistry} appends {@code _total} to counters and {@code _seconds}
 * (+ {@code _count}/{@code _sum}/{@code _max}) to timers automatically on export, so naming them
 * here with those suffixes already baked in would double them up once scraped (SAD 38.3's literal
 * example names, e.g. {@code transactions.received.total}, are the <em>exported</em> Prometheus
 * names this class's Java-side names are chosen to produce, not the Java-side names themselves).
 *
 * <p>Counters for the three {@link RejectionReason} values are pre-built once, in the
 * constructor, into a fixed {@link EnumMap} - the tag's value set is exactly as bounded as the
 * enum itself, never constructed from a caller-supplied string at call time.
 */
@Component
class MicrometerTransactionMetrics implements TransactionMetricsPort {

	private final MeterRegistry meterRegistry;
	private final Counter received;
	private final Counter processed;
	private final Counter duplicateRejected;
	private final Counter categorisationFallback;
	private final Timer processingDuration;
	private final Map<RejectionReason, Counter> rejectedByReason;

	MicrometerTransactionMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
		this.received = Counter.builder("transactions.received")
				.description("Transaction ingestion attempts (single or bulk-item), before validation.")
				.register(meterRegistry);
		this.processed = Counter.builder("transactions.processed")
				.description("Transactions whose creation workflow reached its completed normal success path.")
				.register(meterRegistry);
		this.duplicateRejected = Counter.builder("transactions.duplicates")
				.description("Transactions rejected as duplicates (BR-08).")
				.register(meterRegistry);
		this.categorisationFallback = Counter.builder("transactions.categorisation.fallback")
				.description("Successfully processed transactions whose assigned category was the fallback category.")
				.register(meterRegistry);
		this.processingDuration = Timer.builder("transactions.processing.duration")
				.description("Duration of one transaction-processing attempt, every outcome.")
				.register(meterRegistry);

		this.rejectedByReason = new EnumMap<>(RejectionReason.class);
		for (RejectionReason reason : RejectionReason.values()) {
			rejectedByReason.put(reason, Counter.builder("transactions.rejected")
					.description("Transactions rejected for a non-duplicate reason.")
					.tag("reason", reason.name())
					.register(meterRegistry));
		}
	}

	@Override
	public void transactionReceived() {
		received.increment();
	}

	@Override
	public void transactionProcessed() {
		processed.increment();
	}

	@Override
	public void transactionRejected(RejectionReason reason) {
		rejectedByReason.get(reason).increment();
	}

	@Override
	public void transactionDuplicateRejected() {
		duplicateRejected.increment();
	}

	@Override
	public void categorisationFallback() {
		categorisationFallback.increment();
	}

	@Override
	public ProcessingTimer startProcessingTimer() {
		Timer.Sample sample = Timer.start(meterRegistry);
		return () -> sample.stop(processingDuration);
	}

}
