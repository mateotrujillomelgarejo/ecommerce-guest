package pe.takiq.ecommerce.cart_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PriceCalculationResponse {

    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal total;
    private List<ItemPrice> items;
    private String appliedCoupon;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemPrice {
        private String productId;
        private BigDecimal unitPrice;
        private BigDecimal discountedUnitPrice;
        private Integer quantity;
        private BigDecimal lineTotal;
    }
}
