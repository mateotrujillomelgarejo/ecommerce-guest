package pe.takiq.ecommerce.order_service.dto;

import lombok.Data;

@Data
public class CartItemDTO {
    private String productId;
    private String productName;
    private double price;
    private int quantity;
}
