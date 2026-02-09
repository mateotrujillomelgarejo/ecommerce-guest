package pe.takiq.ecommerce.order_service.Listener;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pe.takiq.ecommerce.order_service.config.RabbitMQConfig;
import pe.takiq.ecommerce.order_service.event.InventoryFailedEvent;
import pe.takiq.ecommerce.order_service.model.OrderStatus;
import pe.takiq.ecommerce.order_service.service.OrderService;

@Component
@RequiredArgsConstructor
public class InventoryEventListener {

    private final OrderService orderService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_INVENTORY_FAILED)
    public void handleInventoryFailed(InventoryFailedEvent event) {
        orderService.updateStatus(event.getOrderId(), OrderStatus.FAILED);
    }
}