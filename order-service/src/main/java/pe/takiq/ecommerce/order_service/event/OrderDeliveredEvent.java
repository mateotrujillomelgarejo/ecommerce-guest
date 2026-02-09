package pe.takiq.ecommerce.order_service.event;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrderDeliveredEvent {
    private String orderId;
    private LocalDateTime deliveredAt;
    private String message;
}