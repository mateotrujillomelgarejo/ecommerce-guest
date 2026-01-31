package pe.takiq.ecommerce.cart_service.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class CartResponseDTO {
    private String id; // ahora sessionId
    private List<CartItemResponseDTO> items;
    private Double subtotal;
    private Double discount;
    private Double tax;
    private Double shippingEstimate;
    private Double total;
}