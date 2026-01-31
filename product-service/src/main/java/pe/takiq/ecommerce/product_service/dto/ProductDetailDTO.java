package pe.takiq.ecommerce.product_service.dto;

import lombok.Data;
import pe.takiq.ecommerce.product_service.model.Product;

@Data
public class ProductDetailDTO {
    private Product product;
    private Boolean available;
    private String stockMessage;
}