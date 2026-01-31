package pe.takiq.ecommerce.cart_service.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class PriceCalculationRequest {

    private List<CartItem> items;
    private String couponCode;   // opcional
    private String customerType; // GUEST, REGISTERED, VIP

    @Data
    public static class CartItem {
        private String productId;
        private Integer quantity;
    }
}
