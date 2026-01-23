package pe.takiq.ecommerce.cart_service.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class CartResponseDTO {
    private String id;
    private List<CartItemResponseDTO> items;
    private Double total;
}
