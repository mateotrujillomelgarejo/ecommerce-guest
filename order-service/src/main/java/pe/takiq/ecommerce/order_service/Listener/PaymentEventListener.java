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

import java.math.BigDecimal;

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
            return;
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaymentId(event.getPaymentId());
        orderRepository.save(order);

        OrderCreatedEvent createdEvent = OrderCreatedEvent.builder()
                .orderId(order.getId())
                .guestId(order.getGuestId())
                .sessionId(order.getSessionId())
                .guestEmail(order.getGuestEmail())
                .total(order.getTotalAmount())
                .items(order.getItems().stream().map(item -> 
                    OrderCreatedEvent.OrderItemEvent.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .unitPriceSnapshot(BigDecimal.valueOf(item.getPrice() != null ? item.getPrice() : 0.0))
                        .build()
                ).toList())
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "order.created", createdEvent);
    }
}