package pe.takiq.ecommerce.order_service.Listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import pe.takiq.ecommerce.order_service.config.RabbitMQConfig;
import pe.takiq.ecommerce.order_service.event.OrderShippedEvent;
import pe.takiq.ecommerce.order_service.service.OrderService;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShippingEventListener {

    private final OrderService orderService;
    private final StringRedisTemplate redisTemplate;

@RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_SHIPPED)
public void handleOrderShipped(OrderShippedEvent event) {

    String inboxKey = "inbox:shipping:shipped:" + event.getOrderId();

    Boolean isFirstTime = redisTemplate.opsForValue()
            .setIfAbsent(inboxKey, "DONE", Duration.ofDays(7));

    if (Boolean.FALSE.equals(isFirstTime)) {
        log.info("Evento SHIPPED {} ya procesado.", event.getOrderId());
        return;
    }

    try {

        orderService.markAsShipped(event.getOrderId(), event.getTrackingNumber());
        log.info("Orden {} actualizada a SHIPPED con tracking {}", event.getOrderId(), event.getTrackingNumber());

    } catch (Exception e) {

        redisTemplate.delete(inboxKey); // permitir retry

        log.error("Error procesando SHIPPED {}. Se permite reintento.",
                event.getOrderId(), e);

        throw e;
    }
}

}