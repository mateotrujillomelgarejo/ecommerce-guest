package pe.takiq.ecommerce.shipping_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.takiq.ecommerce.events.OrderCreatedEvent;
import pe.takiq.ecommerce.shipping_service.config.RabbitMQConfig;
import pe.takiq.ecommerce.shipping_service.service.ShippingService;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final ShippingService shippingService;

    @RabbitListener(queues = RabbitMQConfig.SHIPPING_CREATED_QUEUE)
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Shipping recibido OrderCreated → creando envío para orden: {}", event.getOrderId());
        shippingService.createShipmentFromOrder(event);
    }
    
}