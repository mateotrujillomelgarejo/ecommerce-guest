package pe.takiq.ecommerce.payment_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.takiq.ecommerce.payment_service.model.PaymentOutboxEvent;
import java.util.List;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutboxEvent, String> {
    List<PaymentOutboxEvent> findByProcessedFalse();
}