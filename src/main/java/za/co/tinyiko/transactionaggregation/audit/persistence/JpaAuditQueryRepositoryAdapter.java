package za.co.tinyiko.transactionaggregation.audit.persistence;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import za.co.tinyiko.transactionaggregation.audit.port.AuditEventRow;
import za.co.tinyiko.transactionaggregation.audit.port.AuditEventSearchPage;
import za.co.tinyiko.transactionaggregation.audit.port.AuditEventSearchQuery;
import za.co.tinyiko.transactionaggregation.audit.port.AuditQueryRepositoryPort;

@Component
class JpaAuditQueryRepositoryAdapter implements AuditQueryRepositoryPort {

	private final SpringDataAuditSearchRepository springDataAuditSearchRepository;

	JpaAuditQueryRepositoryAdapter(SpringDataAuditSearchRepository springDataAuditSearchRepository) {
		this.springDataAuditSearchRepository = springDataAuditSearchRepository;
	}

	@Override
	public AuditEventSearchPage search(AuditEventSearchQuery query) {
		List<Specification<AuditEventEntity>> specifications = Stream.of(
				AuditEventSpecifications.hasAggregateType(query.aggregateType()),
				AuditEventSpecifications.hasAggregateId(query.aggregateId()),
				AuditEventSpecifications.hasEventType(query.eventType()),
				AuditEventSpecifications.hasActor(query.actor()),
				AuditEventSpecifications.hasCorrelationId(query.correlationId()),
				AuditEventSpecifications.occurredBetween(query.occurredFrom(), query.occurredTo())
		).filter(Objects::nonNull).toList();
		Specification<AuditEventEntity> specification = Specification.allOf(specifications);

		Sort sort = Sort.by(query.sortAscending() ? Sort.Direction.ASC : Sort.Direction.DESC, "occurredAt");
		PageRequest pageable = PageRequest.of(query.page(), query.size(), sort);

		Page<AuditEventEntity> page = springDataAuditSearchRepository.findAll(specification, pageable);
		List<AuditEventRow> rows = page.getContent().stream().map(JpaAuditQueryRepositoryAdapter::toRow).toList();
		return new AuditEventSearchPage(rows, page.getTotalElements());
	}

	private static AuditEventRow toRow(AuditEventEntity entity) {
		return new AuditEventRow(
				entity.getId(),
				entity.getAggregateType(),
				entity.getAggregateId(),
				entity.getEventType(),
				entity.getActor(),
				entity.getCorrelationId(),
				entity.getEventData(),
				entity.getOccurredAt());
	}

}
