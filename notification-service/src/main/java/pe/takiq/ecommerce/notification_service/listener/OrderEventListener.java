package pe.takiq.ecommerce.notification_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import pe.takiq.ecommerce.notification_service.config.RabbitMQConfig;
import pe.takiq.ecommerce.notification_service.events.OrderCreatedEvent;
import pe.takiq.ecommerce.notification_service.events.OrderShippedEvent;
import pe.takiq.ecommerce.notification_service.service.EmailNotificationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final EmailNotificationService emailService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Enviando email de confirmación de pedido recibido y pagado → orderId: {}", event.getOrderId());
        emailService.sendOrderCreatedNotification(event);
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_SHIPPED_QUEUE)
    public void onOrderShipped(OrderShippedEvent event) {
        log.info("Enviando email de envío → orderId: {}, tracking: {}", 
                 event.getOrderId(), event.getTrackingNumber());
        emailService.sendShippingConfirmation(event);
    }

    // Opcional – si Shipping publica order.delivered en el futuro
    /*
    @RabbitListener(queues = "notification.order-delivered.queue")
    public void onOrderDelivered(OrderDeliveredEvent event) {
        emailService.sendOrderDeliveredNotification(event);
    }
    */
}