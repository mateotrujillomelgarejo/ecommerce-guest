package pe.takiq.ecommerce.order_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmedEvent {
    private String orderId;
    private String cartId;
    private String guestEmail;
    private Double totalAmount;
    private String paymentId;
    private LocalDateTime confirmedAt;
    private String status;
}