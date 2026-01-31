package pe.takiq.ecommerce.inventory_service.event;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.takiq.ecommerce.events.OrderConfirmedEvent;
import pe.takiq.ecommerce.events.OrderCreatedEvent;
import pe.takiq.ecommerce.inventory_service.config.RabbitMQConfig;
import pe.takiq.ecommerce.inventory_service.service.InventoryService;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final InventoryService inventoryService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    @Retry(name = "rabbitmqRetry")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Procesando OrderCreated → reserva stock | orderId={}", event.getOrderId());
        inventoryService.reserveFromOrder(event);
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_CONFIRMED_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    @Retry(name = "rabbitmqRetry")
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Procesando OrderConfirmed → commit stock | orderId={}, paymentId={}", 
                event.getOrderId(), event.getPaymentId());
        inventoryService.commitFromOrder(event);
    }
}