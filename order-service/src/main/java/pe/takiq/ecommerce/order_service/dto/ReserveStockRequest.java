package pe.takiq.ecommerce.order_service.dto;

import lombok.Data;
import java.util.List;

@Data
public class ReserveStockRequest {
    private String orderId;
    private List<Item> items;

    @Data
    public static class Item {
        private String productId;
        private Integer quantity;
    }
}