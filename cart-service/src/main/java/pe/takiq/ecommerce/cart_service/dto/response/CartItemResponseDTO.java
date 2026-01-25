package pe.takiq.ecommerce.cart_service.dto.response;

import lombok.Data;

@Data
public class CartItemResponseDTO {
    private String productId;
    private String productName;
    private Double price;
    private Integer quantity;
    private Double subtotal;
}
