package za.co.tinyiko.transactionaggregation.categorisation.port;

import java.util.List;
import java.util.Optional;

import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRule;
import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRuleId;

/**
 * Outbound port the categorisation application layer uses to read and, since
 * {@code feature/category-admin}, write rules.
 *
 * <p>{@link #findAllActive()} is the runtime categorisation read path, unchanged since
 * {@code feature/categorisation} - untouched by admin writes. The admin read/write methods
 * ({@link #findAll()}, {@link #findRowById}, {@link #create}, {@link #update}) all return
 * {@link CategorisationRuleRow}, this port's own shape, never the domain {@link CategorisationRule}
 * for admin reads - the row carries {@code version}, a persistence concept the domain aggregate
 * deliberately never learns about.
 *
 * <p>{@link #update} takes {@code expectedVersion} as a plain {@code long} and is solely
 * responsible, inside the adapter, for the entire load -&gt; compare -&gt; mutate -&gt; flush
 * sequence against one single managed entity (see {@code JpaCategorisationRuleRepositoryAdapter}'s
 * own Javadoc for exactly why this must not be split across two separate port calls).
 */
public interface CategorisationRuleRepositoryPort {

	/**
	 * All active rules. Not required to be pre-sorted by priority - the rule engine sorts
	 * defensively - but the adapter does order by priority anyway, since that's exactly what
	 * the underlying index (idx_categorisation_rules_active_priority) is for.
	 */
	List<CategorisationRule> findAllActive();

	List<CategorisationRuleRow> findAll();

	Optional<CategorisationRuleRow> findRowById(CategorisationRuleId id);

	CategorisationRuleRow create(CategorisationRule rule);

	CategorisationRuleRow update(CategorisationRuleId id, CategorisationRule updatedFields, long expectedVersion);

}
