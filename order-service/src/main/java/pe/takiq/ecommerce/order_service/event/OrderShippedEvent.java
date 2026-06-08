package pe.takiq.ecommerce.order_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderShippedEvent {
    private String orderId;
    private String trackingNumber;
    private Instant shippedAt;
    private String guestEmail;
    private String carrier;
    private String estimatedDelivery;
    private String message;
}