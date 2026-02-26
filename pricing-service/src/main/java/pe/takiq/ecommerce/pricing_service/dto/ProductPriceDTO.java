package pe.takiq.ecommerce.pricing_service.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductPriceDTO {
    private String id;
    private BigDecimal price;
}