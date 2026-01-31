package pe.takiq.ecommerce.order_service.dto.response;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ShippingCalculationResponse {
    private BigDecimal shippingCost;
    private String estimatedDelivery;   // ej: "3-5 días hábiles"
    private String message;             // ej: "Envío gratis por superar S/ 300"
}