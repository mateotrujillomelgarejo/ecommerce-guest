package pe.takiq.ecommerce.shipping_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShippingCalculationResponse {
    private BigDecimal shippingCost;
    private String estimatedDelivery;   // ej: "3-5 días hábiles"
    private String message;             // ej: "Envío gratis por superar S/ 300"
}