package pe.takiq.ecommerce.order_service.event;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrderCancelledEvent {

    private String orderId;
    private String reason;
    private LocalDateTime cancelledAt;
}