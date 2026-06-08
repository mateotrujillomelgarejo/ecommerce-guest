package pe.takiq.ecommerce.order_service.event;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class OrderCancelledEvent {

    private String orderId;
    private String sessionId;
    private String reason;
    private Instant cancelledAt;
}