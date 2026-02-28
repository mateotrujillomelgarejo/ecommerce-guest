package pe.takiq.ecommerce.order_service.Listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pe.takiq.ecommerce.order_service.config.RabbitMQConfig;
import pe.takiq.ecommerce.order_service.event.OrderPaidEvent;
import pe.takiq.ecommerce.order_service.event.PaymentSucceededEvent;
import pe.takiq.ecommerce.order_service.model.Order;
import pe.takiq.ecommerce.order_service.model.OrderStatus;
import pe.takiq.ecommerce.order_service.model.OutboxEvent;
import pe.takiq.ecommerce.order_service.repository.OrderRepository;
import pe.takiq.ecommerce.order_service.repository.OutboxEventRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

@Transactional
@RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_SUCCEEDED)
public void handlePaymentSucceeded(PaymentSucceededEvent event) throws Exception {

    String inboxKey = "inbox:payment:" + event.getPaymentId();
    Boolean isFirstTime = redisTemplate.opsForValue()
            .setIfAbsent(inboxKey, "DONE", Duration.ofDays(7));

    if (Boolean.FALSE.equals(isFirstTime)) {
        log.info("Pago {} ya fue procesado. Ignorando.", event.getPaymentId());
        return;
    }

    try {

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() ->
                        new RuntimeException("Orden no encontrada: " + event.getOrderId()));

        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            return;
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaymentId(event.getPaymentId());
        orderRepository.save(order);

        OrderPaidEvent createdEvent = OrderPaidEvent.builder()
                .orderId(order.getId())
                .guestId(order.getGuestId())
                .sessionId(order.getSessionId())
                .guestEmail(order.getGuestEmail())
                .total(order.getTotalAmount())
                .items(order.getItems().stream().map(item ->
                        OrderPaidEvent.OrderItemEvent.builder()
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .imageUrl(item.getImageUrl())
                                .quantity(item.getQuantity())
                                .unitPriceSnapshot(
                                        BigDecimal.valueOf(
                                                item.getPrice() != null ? item.getPrice() : 0.0
                                        )
                                )
                                .build()
                ).toList())
                .createdAt(order.getCreatedAt()
                        .atZone(ZoneId.systemDefault())
                        .toInstant())
                .build();

        OutboxEvent outbox = new OutboxEvent();
        outbox.setAggregateId(order.getId());
        outbox.setEventType("order.paid");
        outbox.setPayload(objectMapper.writeValueAsString(createdEvent));
        outboxRepository.save(outbox);

        log.info("Orden {} marcada como PAID y evento enviado a Outbox", order.getId());

    } catch (Exception e) {

        redisTemplate.delete(inboxKey);

        log.error("Error procesando PAYMENT_SUCCEEDED. Se permite reintento.", e);
        throw e;
    }
}
}