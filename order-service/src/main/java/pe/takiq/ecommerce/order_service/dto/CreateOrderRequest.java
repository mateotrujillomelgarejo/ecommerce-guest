package pe.takiq.ecommerce.order_service.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

// dto/CreateOrderRequest.java
@Data
public class CreateOrderRequest {
    private String guestId;
    private String sessionId;
    private String paymentId;         // ← obligatorio ahora
    private BigDecimal shippingCost;
    private BigDecimal total;
    private String guestEmail;
    private List<CartItemDTO> items;  // o solo IDs + cantidades
}