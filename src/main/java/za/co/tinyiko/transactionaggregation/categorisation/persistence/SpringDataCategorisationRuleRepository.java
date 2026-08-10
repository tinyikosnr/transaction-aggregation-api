package za.co.tinyiko.transactionaggregation.categorisation.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataCategorisationRuleRepository extends JpaRepository<CategorisationRuleEntity, UUID> {

	/**
	 * The active-rules read the runtime categorisation path runs unconditionally on every call,
	 * enriched with each rule's target category's {@code is_fallback} flag via an ad hoc
	 * {@code ON} join (Jakarta Persistence 3.1 - {@code categorisation_rules.category_id} is a
	 * scalar column, not a mapped {@code @ManyToOne}) - one query, not two (feature/observability).
	 * Each row is {@code [CategorisationRuleEntity, Boolean]}.
	 */
	@Query("""
			SELECT r, c.fallback FROM CategorisationRuleEntity r
			JOIN TransactionCategoryEntity c ON r.categoryId = c.id
			WHERE r.active = true
			ORDER BY r.priority ASC
			""")
	List<Object[]> findAllActiveWithCategoryFallback();

}
