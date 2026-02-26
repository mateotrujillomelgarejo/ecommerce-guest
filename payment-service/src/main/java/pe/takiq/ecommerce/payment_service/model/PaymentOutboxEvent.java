package pe.takiq.ecommerce.payment_service.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "payment_outbox_events")
public class PaymentOutboxEvent {
    @Id
    private String id = UUID.randomUUID().toString();
    private String eventType;
    
    @Column(columnDefinition = "TEXT")
    private String payload;
    
    private LocalDateTime createdAt = LocalDateTime.now();
    private boolean processed = false;
}