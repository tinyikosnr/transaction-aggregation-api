/**
 * Transaction module - owns {@code transactions} and {@code transaction_sources}.
 *
 * <p>Responsible for transaction ingestion, validation, duplicate detection, persistence and
 * search. Depends on the customer, merchant, categorisation and audit modules' application
 * interfaces, plus {@code shared} for {@code CorrelationId}. No other module may access this
 * module's {@code persistence} package directly.
 *
 * <p>{@code allowedDependencies} is explicit here, unlike {@code customer}/{@code merchant}:
 * this is the first module with a genuinely multi-way dependency list, making the enforcement
 * especially valuable - without it, a stray import into {@code aggregation} or {@code security}
 * would go undetected by {@code ModularityTests}.
 *
 * <p>Each business-module entry uses the qualified {@code "module :: application"} syntax, not
 * a bare module name: a bare name only grants access to that module's default (root) interface,
 * not a specifically-named one - and {@code customer.application}/{@code merchant.application}/
 * {@code categorisation.application}/{@code audit.application} each expose their API via a
 * {@code @NamedInterface} named {@code "application"} (derived from the package's own simple
 * name), not the default interface. {@code verify()} caught this immediately when the plain
 * module names were tried first - trust the failing test over the shorter-looking syntax.
 * {@code shared} stays a bare name: it is {@code type = OPEN}, so every one of its packages,
 * named interface or not, is accessible regardless.
 */
@org.springframework.modulith.ApplicationModule(
		allowedDependencies = {
				"customer :: application",
				"merchant :: application",
				"categorisation :: application",
				"audit :: application",
				"shared"
		})
package za.co.tinyiko.transactionaggregation.transaction;
