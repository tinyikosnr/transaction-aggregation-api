package za.co.tinyiko.transactionaggregation.customer.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCustomerRepository extends JpaRepository<CustomerEntity, UUID> {

	Optional<CustomerEntity> findByExternalReference(String externalReference);

}
