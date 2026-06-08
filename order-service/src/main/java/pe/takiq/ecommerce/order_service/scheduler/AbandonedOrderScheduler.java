package pe.takiq.ecommerce.order_service.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pe.takiq.ecommerce.order_service.event.OrderCancelledEvent;
import pe.takiq.ecommerce.order_service.model.Order;
import pe.takiq.ecommerce.order_service.model.OrderStatus;
import pe.takiq.ecommerce.order_service.model.OutboxEvent;
import pe.takiq.ecommerce.order_service.repository.OrderRepository;
import pe.takiq.ecommerce.order_service.repository.OutboxEventRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AbandonedOrderScheduler {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cancelAbandonedOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);

        List<Order> abandonedOrders = orderRepository.findByStatus(OrderStatus.PAYMENT_PENDING)
                .stream()
                .filter(o -> o.getCreatedAt().isBefore(threshold))
                .toList();

        for (Order order : abandonedOrders) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setFailureReason("Expiración de tiempo de pago (Timeout)");
            orderRepository.save(order);

            try {
                OrderCancelledEvent cancelEvent = OrderCancelledEvent.builder()
                        .orderId(order.getId())
                        .sessionId(order.getSessionId())
                        .reason("Timeout de pago")
                        .cancelledAt(Instant.now())
                        .build();

                OutboxEvent outbox = new OutboxEvent();
                outbox.setAggregateId(order.getId());
                outbox.setEventType("order.cancelled");
                outbox.setPayload(objectMapper.writeValueAsString(cancelEvent));
                outboxRepository.save(outbox);

                log.info("Orden abandonada {} marcada CANCELLED. Evento order.cancelled en Outbox.",
                        order.getId());
            } catch (Exception e) {
                log.error("Error guardando order.cancelled en Outbox para orden {}: {}",
                        order.getId(), e.getMessage());
            }
        }
    }
}