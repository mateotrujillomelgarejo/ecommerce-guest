package pe.takiq.ecommerce.cart_service.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    private String productId;
    private String productName;
    private Double price;
    private Integer quantity;
    private String imageUrl;
}