package pe.takiq.ecommerce.order_service.Listener;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import pe.takiq.ecommerce.order_service.config.RabbitMQConfig;
import pe.takiq.ecommerce.order_service.event.OrderCreatedEvent;
import pe.takiq.ecommerce.order_service.event.PaymentSucceededEvent;
import pe.takiq.ecommerce.order_service.model.Order;
import pe.takiq.ecommerce.order_service.model.OrderStatus;
import pe.takiq.ecommerce.order_service.repository.OrderRepository;

@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_SUCCEEDED)
    public void handlePaymentSucceeded(PaymentSucceededEvent event) {
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + event.getOrderId()));

        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            return; // Idempotencia: ya procesado
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaymentId(event.getPaymentId());
        orderRepository.save(order);

        // SOLO AQUÍ: Publicar order.created tras pago
        OrderCreatedEvent createdEvent = OrderCreatedEvent.builder()
                .orderId(order.getId())
                .guestId(order.getGuestId())
                .total(order.getTotalAmount())
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "order.created", createdEvent);
    }
}