package pe.takiq.ecommerce.order_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.takiq.ecommerce.order_service.model.OutboxEvent;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    List<OutboxEvent> findByProcessedFalse();
}