package pe.takiq.ecommerce.order_service.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentSucceededEvent {
    private String paymentId;
    private String orderId;
    private Double amount;
}