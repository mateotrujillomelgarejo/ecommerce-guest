package pe.takiq.ecommerce.order_service.Listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import pe.takiq.ecommerce.order_service.config.RabbitMQConfig;
import pe.takiq.ecommerce.order_service.event.InventoryFailedEvent;
import pe.takiq.ecommerce.order_service.service.OrderService;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventListener {

    private final OrderService orderService;
    private final StringRedisTemplate redisTemplate;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_INVENTORY_FAILED)
    public void handleInventoryFailed(InventoryFailedEvent event) {
        
        String inboxKey = "inbox:inventory:failed:" + event.getOrderId();
        Boolean isFirstTime = redisTemplate.opsForValue()
                .setIfAbsent(inboxKey, "DONE", Duration.ofDays(7));

        if (Boolean.FALSE.equals(isFirstTime)) {
            log.info("Evento INVENTORY_FAILED para orden {} ya procesado. Ignorando duplicado.", event.getOrderId());
            return;
        }

        try {
            orderService.markAsFailed(event.getOrderId(), event.getReason());
            log.info("Orden {} marcada como FAILED por problema de inventario. Razón: {}", event.getOrderId(), event.getReason());

        } catch (Exception e) {
            redisTemplate.delete(inboxKey);
            log.error("Error procesando INVENTORY_FAILED para orden {}. Se permite reintento.", event.getOrderId(), e);
            throw e;
        }
    }
}