package pe.takiq.ecommerce.order_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InventoryFailedEvent {
    private String orderId;
    private String reason;
}