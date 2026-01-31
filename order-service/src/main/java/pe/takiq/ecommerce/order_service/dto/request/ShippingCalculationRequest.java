package pe.takiq.ecommerce.order_service.dto.request;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ShippingCalculationRequest {
    private BigDecimal orderTotal;     // Subtotal o total antes de envío
    private String postalCode;         // Para futura lógica por zona
    private Integer itemCount;         // Opcional: por cantidad de productos
}