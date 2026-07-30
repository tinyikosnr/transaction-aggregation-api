package za.co.tinyiko.transactionaggregation.customer.mapper;

import za.co.tinyiko.transactionaggregation.customer.domain.Customer;
import za.co.tinyiko.transactionaggregation.customer.domain.CustomerId;
import za.co.tinyiko.transactionaggregation.customer.persistence.CustomerEntity;

/**
 * Maps between the domain {@link Customer} and the JPA {@link CustomerEntity}.
 *
 * <p>{@link #applyTo} rather than a {@code toEntity} that returns a new instance: version-based
 * optimistic locking needs Hibernate's own managed entity to be mutated in place, not replaced
 * with a detached copy that would carry a stale or absent version. The caller is responsible
 * for supplying either an already-managed entity (update) or a freshly-constructed one (insert).
 */
public final class CustomerMapper {

	private CustomerMapper() {
	}

	public static Customer toDomain(CustomerEntity entity) {
		return Customer.reconstitute(
				new CustomerId(entity.getId()),
				entity.getExternalReference(),
				entity.getFirstName(),
				entity.getLastName(),
				entity.getEmailAddress(),
				entity.getStatus(),
				entity.getCreatedAt(),
				entity.getUpdatedAt());
	}

	public static void applyTo(CustomerEntity entity, Customer customer) {
		entity.setId(customer.id().value());
		entity.setExternalReference(customer.externalReference());
		entity.setFirstName(customer.firstName());
		entity.setLastName(customer.lastName());
		entity.setEmailAddress(customer.emailAddress());
		entity.setStatus(customer.status());
		entity.setCreatedAt(customer.createdAt());
		entity.setUpdatedAt(customer.updatedAt());
	}

}
