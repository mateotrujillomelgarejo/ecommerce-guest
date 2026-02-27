package pe.takiq.ecommerce.shipping_service.listener;

import java.time.Duration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import pe.takiq.ecommerce.shipping_service.config.RabbitMQConfig;
import pe.takiq.ecommerce.shipping_service.events.OrderPaidEvent;
import pe.takiq.ecommerce.shipping_service.service.ShippingService;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPaidListener {

    private final ShippingService shippingService;
    private final StringRedisTemplate redisTemplate; 

    @RabbitListener(queues = RabbitMQConfig.ORDER_PAID_QUEUE)
    public void onOrderPaid(OrderPaidEvent event) {
        String orderId = event.getOrderId();
        
        // Bloqueo Atómico con Redis (Inbox Pattern)
        String inboxKey = "inbox:shipping:order_paid:" + orderId;
        Boolean isFirstTime = redisTemplate.opsForValue().setIfAbsent(inboxKey, "PROCESSED", Duration.ofDays(7));
        
        if (Boolean.FALSE.equals(isFirstTime)) {
            log.info("Evento OrderPaid para orderId={} ya fue procesado. Ignorando duplicado.", orderId);
            return;
        }

        log.info("OrderPaid recibido → procesando envío para orderId={}", orderId);

        try {
            if (shippingService.existsShipment(orderId)) {
                log.info("Shipment ya existe en BD para {}, ignorando", orderId);
                return;
            }

            shippingService.createAndShip(event);
            
        } catch (Exception e) {
            // Si falla, borramos la llave de Redis para permitir que RabbitMQ reintente
            redisTemplate.delete(inboxKey);
            log.error("Error creando shipment para orderId={}. Se reintentará.", orderId, e);
            throw e; 
        }
    }
}