package pe.takiq.ecommerce.order_service.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    private String id = UUID.randomUUID().toString();
    private String aggregateId;   // Ej: el orderId
    private String eventType;     // Ej: "order.created"
    
    @Column(columnDefinition = "VARCHAR(MAX)") // Sintaxis correcta para SQL Server
    private String payload;       // El JSON del evento
    
    private LocalDateTime createdAt = LocalDateTime.now();
    private boolean processed = false;
}