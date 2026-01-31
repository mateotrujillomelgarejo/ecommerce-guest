package pe.takiq.ecommerce.order_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;
import pe.takiq.ecommerce.order_service.model.Order;

@Data
public class OrderResponseDTO {
    private String orderId;
    private Order.OrderStatus status;
    private BigDecimal total;
    private String trackingNumber;
    private LocalDateTime createdAt;
    // + items resumidos, etc.
}