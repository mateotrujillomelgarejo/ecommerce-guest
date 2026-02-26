package pe.takiq.ecommerce.inventory_service.listener;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.takiq.ecommerce.inventory_service.config.RabbitMQConfig;
import pe.takiq.ecommerce.inventory_service.event.InventoryFailedEvent;
import pe.takiq.ecommerce.inventory_service.event.OrderPaidEvent;
import pe.takiq.ecommerce.inventory_service.event.OrderCancelledEvent; // Crear este DTO
import pe.takiq.ecommerce.inventory_service.service.InventoryService;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventListener {

    private final InventoryService inventoryService;
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @RabbitListener(queues = RabbitMQConfig.ORDER_PAID_QUEUE)
    public void onOrderPaid(OrderPaidEvent event) {
        String inboxKey = "inbox:inventory:order_paid:" + event.getOrderId();
        Boolean isFirstTime = redisTemplate.opsForValue().setIfAbsent(inboxKey, "PROCESSED", Duration.ofDays(7));
        
        if (Boolean.FALSE.equals(isFirstTime)) {
            log.info("Evento de pago para orderId={} ya fue procesado (Idempotencia).", event.getOrderId());
            return;
        }

        log.info("OrderPaid recibido → confirmando descuento stock en DB, orderId={}", event.getOrderId());

        try {
            inventoryService.deductStock(event);
        } catch (Exception ex) {
            log.error("Fallo confirmando inventario en DB para orderId={}", event.getOrderId(), ex);
            redisTemplate.delete(inboxKey);

            InventoryFailedEvent failedEvent = InventoryFailedEvent.builder()
                    .orderId(event.getOrderId())
                    .reason(ex.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_EVENTS_EXCHANGE,
                    RabbitMQConfig.INVENTORY_FAILED_ROUTING_KEY,
                    failedEvent
            );
        }
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_CANCELLED_QUEUE)
    public void onOrderCancelled(OrderCancelledEvent event) {
        String inboxKey = "inbox:inventory:order_cancelled:" + event.getOrderId();
        Boolean isFirstTime = redisTemplate.opsForValue().setIfAbsent(inboxKey, "PROCESSED", Duration.ofDays(7));
        
        if (Boolean.FALSE.equals(isFirstTime)) {
            return;
        }

        log.info("OrderCancelled recibido → liberando reserva temporal, orderId={}", event.getOrderId());
        try {
            inventoryService.releaseReservation(event.getOrderId());
        } catch (Exception ex) {
            log.error("Error liberando reserva para orderId={}", event.getOrderId(), ex);
            redisTemplate.delete(inboxKey);
            throw ex;
        }
    }
}