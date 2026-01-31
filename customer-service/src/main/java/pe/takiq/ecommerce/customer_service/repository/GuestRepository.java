package pe.takiq.ecommerce.customer_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.takiq.ecommerce.customer_service.model.Guest;

import java.util.Optional;

public interface GuestRepository extends JpaRepository<Guest, String> {
    Optional<Guest> findBySessionId(String sessionId);
    Optional<Guest> findByEmail(String email);
}