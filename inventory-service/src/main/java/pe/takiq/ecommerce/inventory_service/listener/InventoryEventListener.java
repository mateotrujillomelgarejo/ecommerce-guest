package pe.takiq.ecommerce.inventory_service.listener;

import java.time.LocalDateTime;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.takiq.ecommerce.inventory_service.config.RabbitMQConfig;
import pe.takiq.ecommerce.inventory_service.event.InventoryFailedEvent;
import pe.takiq.ecommerce.inventory_service.event.OrderCreatedEvent;
import pe.takiq.ecommerce.inventory_service.service.InventoryService;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventListener {

    private final InventoryService inventoryService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("OrderCreated recibido → descontando stock, orderId={}", event.getOrderId());

        try {
            inventoryService.deductStock(event);
        } catch (Exception ex) {
            log.error("Fallo inventario para orderId={}", event.getOrderId(), ex);

            InventoryFailedEvent failedEvent = InventoryFailedEvent.builder()
                    .orderId(event.getOrderId())
                    .reason(ex.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_EVENTS_EXCHANGE,
                    RabbitMQConfig.INVENTORY_FAILED_ROUTING_KEY,
                    failedEvent
            );
        }
    }
}
