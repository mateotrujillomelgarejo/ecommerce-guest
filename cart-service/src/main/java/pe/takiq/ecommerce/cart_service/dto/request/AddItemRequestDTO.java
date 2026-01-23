package pe.takiq.ecommerce.cart_service.dto.request;

import lombok.Data;

@Data
public class AddItemRequestDTO {
    private Long productId;
    private Integer quantity;
}
