package pe.takiq.ecommerce.notification_service.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmedEvent {

    private String orderId;
    private String cartId;

    private String sessionId;
    private String guestId; 

    private String guestEmail;
    private Double totalAmount;
    private String paymentId;
    private LocalDateTime confirmedAt;
    private String status;

    private List<OrderItemEvent> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemEvent {
        private String productId;
        private Integer quantity;
    }
}