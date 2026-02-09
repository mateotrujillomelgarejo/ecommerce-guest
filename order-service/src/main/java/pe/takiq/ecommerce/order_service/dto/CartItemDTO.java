package pe.takiq.ecommerce.order_service.dto;

import lombok.Data;

@Data
public class CartItemDTO {
    private String productId;
    private Integer quantity;
    private Double price;
}