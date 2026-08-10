package za.co.tinyiko.transactionaggregation.categorisation.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import za.co.tinyiko.transactionaggregation.categorisation.application.RuleConflictException;
import za.co.tinyiko.transactionaggregation.categorisation.application.RuleNotFoundException;
import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRule;
import za.co.tinyiko.transactionaggregation.categorisation.domain.CategorisationRuleId;
import za.co.tinyiko.transactionaggregation.categorisation.mapper.CategorisationRuleMapper;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategorisationRuleRepositoryPort;
import za.co.tinyiko.transactionaggregation.categorisation.port.CategorisationRuleRow;

/**
 * {@link #update} is the one method in this codebase where the exact sequencing matters enough
 * to spell out precisely (feature/category-admin): it must load <strong>one</strong> managed
 * {@link CategorisationRuleEntity}, compare the caller's {@code expectedVersion} against
 * <em>that same, just-loaded</em> entity's version, mutate <em>that same instance</em>, and flush
 * it - never a second, separate {@code findById} call in between. Splitting the version
 * comparison and the mutation across two separate reads would let a concurrent write that lands
 * between them go undetected: the second read would silently pick up the newer version, the
 * caller's stale {@code expectedVersion} would then "coincidentally" match the freshly-reloaded
 * value, and the update would wrongly succeed. Because there is only ever one load here, that
 * failure mode cannot happen: the entity's version field is never refreshed from the database
 * between the explicit check and the flush, so
 * <ul>
 *   <li>the explicit {@code entity.getVersion() != expectedVersion} check catches the common,
 *       expected case - the caller read the version some time ago and it has since changed;</li>
 *   <li>{@code saveAndFlush}'s own Hibernate-generated {@code UPDATE ... WHERE id = ? AND
 *       version = ?} - built from that <em>same</em> entity instance's version, not a re-read one
 *       - is the final, concurrency-safe guarantee for a write that lands in the narrow window
 *       between this method's own {@code findById} and its {@code saveAndFlush} (SAD 32.6:
 *       "conflicts are detected rather than silently overwritten").</li>
 * </ul>
 * Both failure paths translate to the same {@link RuleConflictException} (409
 * {@code OPTIMISTIC_LOCK_CONFLICT}). This holds regardless of whether this method executes inside
 * an outer {@code @Transactional} service boundary (the entity then simply stays attached and
 * managed for the whole method) or is called with no ambient transaction at all (each Spring Data
 * call opens its own; the entity becomes detached after {@code findById}, and {@code saveAndFlush}
 * then performs a JPA {@code merge}, which enforces the identical version-vs-current-row check at
 * merge time) - proved directly by
 * {@code JpaCategorisationRuleRepositoryAdapterConcurrencyTests}, which exercises the latter case
 * with two genuinely independent, separately-committing calls.
 *
 * <p>No blanket {@code DataIntegrityViolationException} translation exists here: rules have no
 * uniqueness constraint (duplicates are explicitly permitted), so there is no other persistence
 * failure with a defined application meaning to translate on {@link #create} - an unexpected
 * constraint or database failure there is left to propagate as a genuine systemic error, not
 * disguised as a business conflict.
 */
@Component
class JpaCategorisationRuleRepositoryAdapter implements CategorisationRuleRepositoryPort {

	private final SpringDataCategorisationRuleRepository springDataCategorisationRuleRepository;

	JpaCategorisationRuleRepositoryAdapter(SpringDataCategorisationRuleRepository springDataCategorisationRuleRepository) {
		this.springDataCategorisationRuleRepository = springDataCategorisationRuleRepository;
	}

	@Override
	public List<CategorisationRule> findAllActive() {
		return springDataCategorisationRuleRepository.findAllByActiveTrue().stream()
				.map(CategorisationRuleMapper::toDomain)
				.toList();
	}

	@Override
	public List<CategorisationRuleRow> findAll() {
		return springDataCategorisationRuleRepository.findAll().stream()
				.map(JpaCategorisationRuleRepositoryAdapter::toRow)
				.toList();
	}

	@Override
	public Optional<CategorisationRuleRow> findRowById(CategorisationRuleId id) {
		return springDataCategorisationRuleRepository.findById(id.value())
				.map(JpaCategorisationRuleRepositoryAdapter::toRow);
	}

	@Override
	public CategorisationRuleRow create(CategorisationRule rule) {
		CategorisationRuleEntity entity = CategorisationRuleMapper.toEntity(rule);
		return toRow(springDataCategorisationRuleRepository.saveAndFlush(entity));
	}

	@Override
	public CategorisationRuleRow update(CategorisationRuleId id, CategorisationRule updatedFields, long expectedVersion) {
		CategorisationRuleEntity entity = springDataCategorisationRuleRepository.findById(id.value())
				.orElseThrow(() -> new RuleNotFoundException(id.value()));
		if (entity.getVersion() != expectedVersion) {
			throw new RuleConflictException(id.value(), expectedVersion, entity.getVersion());
		}
		CategorisationRuleMapper.applyTo(entity, updatedFields);
		try {
			return toRow(springDataCategorisationRuleRepository.saveAndFlush(entity));
		} catch (ObjectOptimisticLockingFailureException e) {
			throw new RuleConflictException(id.value(), e);
		}
	}

	private static CategorisationRuleRow toRow(CategorisationRuleEntity entity) {
		return new CategorisationRuleRow(
				entity.getId(),
				entity.getCategoryId(),
				entity.getMatchField().name(),
				entity.getOperator().name(),
				entity.getMatchValue(),
				entity.getDirection().name(),
				entity.getPriority(),
				entity.isActive(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
