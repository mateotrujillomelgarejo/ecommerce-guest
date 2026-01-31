package pe.takiq.ecommerce.order_service.dto.request;

import lombok.Data;
import pe.takiq.ecommerce.order_service.dto.response.PriceCalculationResponse;
import pe.takiq.ecommerce.order_service.dto.response.ShippingCalculationResponse;

@Data
public class OrderCreateRequestDTO {
    private String guestId;
    private String cartId;          // o lista de items
    private String paymentId;
    private ShippingCalculationResponse shipping;  // o solo shippingCost
    private PriceCalculationResponse pricing;      // snapshot de precios
}