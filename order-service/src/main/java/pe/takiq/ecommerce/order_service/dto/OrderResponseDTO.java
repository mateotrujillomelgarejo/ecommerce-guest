package pe.takiq.ecommerce.order_service.dto;

import lombok.Data;
import pe.takiq.ecommerce.order_service.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponseDTO {
    private String orderId;
    private String guestId;
    private List<CartItemDTO> items;
    private BigDecimal totalAmount;
    private String paymentId;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private String trackingNumber;
}