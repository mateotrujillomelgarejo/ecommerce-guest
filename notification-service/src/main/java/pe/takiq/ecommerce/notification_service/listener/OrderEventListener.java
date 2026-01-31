package pe.takiq.ecommerce.notification_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import pe.takiq.ecommerce.events.OrderCreatedEvent;
import pe.takiq.ecommerce.events.OrderShippedEvent;
import pe.takiq.ecommerce.events.OrderConfirmedEvent;
import pe.takiq.ecommerce.notification_service.service.EmailNotificationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final EmailNotificationService emailService;

    @RabbitListener(queues = "notification.queue")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Evento OrderCreated recibido - orderId={}", event.getOrderId());
        try {
            emailService.sendOrderCreatedNotification(event);
        } catch (Exception e) {
            log.error("Fallo al enviar email OrderCreated - orderId={}", event.getOrderId(), e);
            throw e;
        }
    }

    @RabbitListener(queues = "notification.queue")
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Evento OrderConfirmed recibido - orderId={}", event.getOrderId());
        try {
            emailService.sendOrderConfirmedNotification(event);
        } catch (Exception e) {
            log.error("Fallo al enviar email OrderConfirmed - orderId={}", event.getOrderId(), e);
            throw e;
        }
    }

    @RabbitListener(queues = "notification.queue")
    public void handleOrderShipped(OrderShippedEvent event) {
        log.info("Evento OrderShipped recibido - orderId={}", event.getOrderId());
        try {
            emailService.sendShippingConfirmation(event.getOrderId(), event.getTrackingNumber());
        } catch (Exception e) {
            log.error("Fallo al enviar email OrderShipped - orderId={}", event.getOrderId(), e);
            throw e;
        }
    }
}