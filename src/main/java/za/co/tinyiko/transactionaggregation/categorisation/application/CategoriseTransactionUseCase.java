package za.co.tinyiko.transactionaggregation.categorisation.application;

/**
 * Inbound port other modules use to assign a category to a transaction (TDS 5). The only
 * capability categorisation exposes - deliberately not split into several interfaces, since
 * TDS documents exactly one operation here.
 *
 * <p>Always returns a decision - there is no "not found" or "failed" outcome to signal. Every
 * direction has a matching catch-all rule (TDS 37), so a category is always assigned, at worst
 * the fallback.
 */
public interface CategoriseTransactionUseCase {

	CategorisationDecision categorise(CategorisationInput input);

}
