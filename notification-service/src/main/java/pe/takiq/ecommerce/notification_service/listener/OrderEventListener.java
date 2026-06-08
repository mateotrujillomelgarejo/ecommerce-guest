package pe.takiq.ecommerce.notification_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import pe.takiq.ecommerce.notification_service.config.RabbitMQConfig;
import pe.takiq.ecommerce.notification_service.events.OrderPaidEvent;
import pe.takiq.ecommerce.notification_service.events.OrderShippedEvent;
import pe.takiq.ecommerce.notification_service.events.UserRegisteredEvent;
import pe.takiq.ecommerce.notification_service.service.EmailNotificationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final EmailNotificationService emailService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_PAID_QUEUE)
    public void onOrderPaid(OrderPaidEvent event) {
        log.info("order.paid recibido → orderId={}", event.getOrderId());
        emailService.sendOrderPaidNotification(event);
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_SHIPPED_QUEUE)
    public void onOrderShipped(OrderShippedEvent event) {
        log.info("order.shipped recibido → orderId={}, tracking={}",
                event.getOrderId(), event.getTrackingNumber());
        emailService.sendShippingConfirmation(event);
    }

    @RabbitListener(queues = RabbitMQConfig.USER_REGISTERED_QUEUE)
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("user.registered recibido → userId={}, email={}",
                event.getUserId(), event.getEmail());
        emailService.sendWelcomeEmail(event);
    }
}