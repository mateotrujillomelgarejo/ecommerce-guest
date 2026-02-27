package pe.takiq.ecommerce.payment_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import pe.takiq.ecommerce.payment_service.config.RabbitMQConfig;
import pe.takiq.ecommerce.payment_service.events.InventoryFailedEvent;
import pe.takiq.ecommerce.payment_service.service.PaymentService;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryFailedListener {

    private final PaymentService paymentService;
    private final StringRedisTemplate redisTemplate;

    @RabbitListener(queues = RabbitMQConfig.INVENTORY_FAILED_QUEUE)
    public void onInventoryFailed(InventoryFailedEvent event) {
        
        String inboxKey = "inbox:payment:refund:" + event.getOrderId();
        Boolean isFirstTime = redisTemplate.opsForValue().setIfAbsent(inboxKey, "DONE", Duration.ofDays(7));

        if (Boolean.FALSE.equals(isFirstTime)) {
            log.info("Reembolso para orden {} ya procesado (Idempotencia). Ignorando duplicado.", event.getOrderId());
            return;
        }

        log.info("SAGA REVERSAL: Recibido evento de inventario fallido para orden {}. Motivo: {}. Iniciando reembolso...", 
                 event.getOrderId(), event.getReason());
                 
        try {
            paymentService.processRefundForFailedOrder(event.getOrderId());
        } catch (Exception e) {
            redisTemplate.delete(inboxKey);
            log.error("Fallo al procesar el reembolso para orden {}. Se reintentará.", event.getOrderId(), e);
            throw e;
        }
    }
}