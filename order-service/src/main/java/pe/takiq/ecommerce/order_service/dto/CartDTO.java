package pe.takiq.ecommerce.order_service.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartDTO {
    private String sessionId;
    private String guestId; // Agregado para asociar guest
    private List<CartItemDTO> items;
    private BigDecimal total;
}