package pe.takiq.ecommerce.cart_service.dto.request;

import lombok.Data;

@Data
public class AddItemRequestDTO {
    private String productId;
    private Integer quantity;
}
