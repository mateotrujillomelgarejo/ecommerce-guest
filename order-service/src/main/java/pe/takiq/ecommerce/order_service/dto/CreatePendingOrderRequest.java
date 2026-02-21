package pe.takiq.ecommerce.order_service.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CreatePendingOrderRequest {
    private String guestId;
    private String sessionId;
    private BigDecimal total;
    private BigDecimal shippingCost;
    private List<CartItemDTO> items;
}