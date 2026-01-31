package pe.takiq.ecommerce.order_service.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PriceCalculationResponse {
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal total;
    private List<ItemPrice> items;

    @Data
    public static class ItemPrice {
        private String productId;
        private BigDecimal unitPrice;
        private BigDecimal discountedUnitPrice;
        private Integer quantity;
        private BigDecimal lineTotal;
    }
}