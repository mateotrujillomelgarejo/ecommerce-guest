package pe.takiq.ecommerce.customer_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.takiq.ecommerce.customer_service.model.Customer;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, String> {
    Optional<Customer> findByEmail(String email);
}