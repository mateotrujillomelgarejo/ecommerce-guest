package pe.takiq.ecommerce.payment_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import pe.takiq.ecommerce.payment_service.events.InventoryFailedEvent;
import pe.takiq.ecommerce.payment_service.service.PaymentService;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryFailedListener {

    private final PaymentService paymentService;
    private final StringRedisTemplate redisTemplate;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "payment.inventory.failed.queue", durable = "true"),
            exchange = @Exchange(value = "ecommerce.events", type = "topic", ignoreDeclarationExceptions = "true"),
            key = "inventory.failed"
    ))
    public void onInventoryFailed(InventoryFailedEvent event) {
        
        String inboxKey = "inbox:payment:refund:" + event.getOrderId();
        Boolean isFirstTime = redisTemplate.opsForValue().setIfAbsent(inboxKey, "DONE", Duration.ofDays(7));

        if (Boolean.FALSE.equals(isFirstTime)) {
            log.info("Reembolso para orden {} ya procesado. Ignorando duplicado.", event.getOrderId());
            return;
        }

        log.info("Recibido evento de inventario fallido para la orden {}. Motivo: {}. Iniciando reembolso...", 
                 event.getOrderId(), event.getReason());
                 
        try {
            paymentService.processRefundForFailedOrder(event.getOrderId());
        } catch (Exception e) {
            redisTemplate.delete(inboxKey);
            throw e;
        }
    }
}