package pe.takiq.ecommerce.cart_service.dto.request;

import lombok.Data;

@Data
public class UpdateItemRequestDTO {
    private String productId;
    private Integer quantity; 
}