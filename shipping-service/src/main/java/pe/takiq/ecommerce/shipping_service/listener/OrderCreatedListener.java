package pe.takiq.ecommerce.shipping_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pe.takiq.ecommerce.shipping_service.config.RabbitMQConfig;
import pe.takiq.ecommerce.shipping_service.events.OrderCreatedEvent;
import pe.takiq.ecommerce.shipping_service.service.ShippingService;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedListener {

    private final ShippingService shippingService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void onOrderCreated(OrderCreatedEvent event) {

        String orderId = event.getOrderId();
        log.info("OrderCreated recibido → shipping, orderId={}", orderId);

        if (shippingService.existsShipment(orderId)) {
            log.info("Shipment ya existe para {}, ignorando", orderId);
            return;
        }

        try {
            shippingService.createAndShip(event);
        } catch (Exception e) {
            log.error("Error creando shipment para {}", orderId, e);
            throw e; // retry + DLQ
        }
    }
}
