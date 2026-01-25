package pe.takiq.ecommerce.inventory_service.event;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pe.takiq.ecommerce.events.OrderConfirmedEvent;
import pe.takiq.ecommerce.inventory_service.config.RabbitMQConfig;
import pe.takiq.ecommerce.inventory_service.service.InventoryService;

@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final InventoryService inventoryService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CONFIRMED_QUEUE)
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        System.out.printf("OrderConfirmedEvent recibido. orderId={}", event.getOrderId());
        inventoryService.decrementStock(event);
    }
}
