package pe.takiq.ecommerce.order_service.dto;

import lombok.Data;
import java.util.List;

@Data
public class CartDTO {
    private String id;
    private List<CartItemDTO> items;
}
