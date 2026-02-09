package pe.takiq.ecommerce.order_service.event;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrderShippedEvent {
    private String orderId;
    private String trackingNumber;
    private LocalDateTime shippedAt;
}