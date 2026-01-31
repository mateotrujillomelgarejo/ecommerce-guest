package pe.takiq.ecommerce.pricing_service.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductPriceDTO {
    private String id;
    private BigDecimal price;
}