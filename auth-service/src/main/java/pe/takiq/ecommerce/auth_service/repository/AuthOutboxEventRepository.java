package pe.takiq.ecommerce.auth_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.takiq.ecommerce.auth_service.domain.AuthOutboxEvent;

import java.util.List;
import java.util.UUID;

public interface AuthOutboxEventRepository extends JpaRepository<AuthOutboxEvent, UUID> {

    List<AuthOutboxEvent> findByProcessedFalse();
}