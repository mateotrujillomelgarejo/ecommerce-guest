package pe.takiq.ecommerce.inventory_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockRestockedEvent {
    private String productId;
    private int quantityAdded;
    private int newAvailableQuantity;
    private String reason;
    private Instant restockedAt;
}