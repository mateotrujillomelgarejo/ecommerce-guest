package pe.takiq.ecommerce.shipping_service.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ShippingCalculationRequest {
    private BigDecimal orderTotal;     // Subtotal o total antes de envío
    private String postalCode;         // Para futura lógica por zona
    private Integer itemCount;         // Opcional: por cantidad de productos
}