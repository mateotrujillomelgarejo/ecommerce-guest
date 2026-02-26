package pe.takiq.ecommerce.order_service.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "orders")
public class Order {
    @Id
    private String id = UUID.randomUUID().toString();
    private String guestId;
    
    private String sessionId;  
    private String guestEmail; 
    
    @ElementCollection
    private List<OrderItem> items;
    
    private BigDecimal totalAmount;
    private String paymentId;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PAYMENT_PENDING;
    
    private LocalDateTime createdAt = LocalDateTime.now();

    private String trackingNumber;
    private String failureReason; 

    @Embeddable
    @Data
    public static class OrderItem {
        private String productId;
        private Integer quantity;
        private Double price;
    }
}