package pe.takiq.ecommerce.shipping_service.events;

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
    private String guestEmail;
    private String trackingNumber;
    private String carrier;
    private String estimatedDelivery;
    private Instant shippedAt;
    private String message;
}