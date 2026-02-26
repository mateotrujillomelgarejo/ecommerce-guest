package pe.takiq.ecommerce.payment_service.events;

import lombok.Data;

@Data
public class InventoryFailedEvent {
    private String orderId;
    private String reason;
}