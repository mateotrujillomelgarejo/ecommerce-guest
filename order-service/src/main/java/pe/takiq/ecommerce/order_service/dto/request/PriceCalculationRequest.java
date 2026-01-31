package pe.takiq.ecommerce.order_service.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class PriceCalculationRequest {
    private List<CartItem> items;
    private String couponCode;

    @Data
    public static class CartItem {
        private String productId;
        private Integer quantity;
    }
}