package pe.takiq.ecommerce.notification_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.takiq.ecommerce.notification_service.events.OrderPaidEvent;
import pe.takiq.ecommerce.notification_service.events.OrderShippedEvent;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final TemplateEngine templateEngine;
    private final StringRedisTemplate redisTemplate;
    private final EmailSenderClient emailSenderClient;

    public void sendOrderPaidNotification(OrderPaidEvent event) {
        if (event.getGuestEmail() == null || event.getGuestEmail().isBlank()) return;

        String lockKey = "lock:email:paid:" + event.getOrderId();
        Boolean isFirstTime = redisTemplate.opsForValue().setIfAbsent(lockKey, "SENT", Duration.ofDays(7));
        
        if (Boolean.FALSE.equals(isFirstTime)) {
            log.info("El correo de Pago Aprobado de la orden {} ya se envió. Ignorando duplicado.", event.getOrderId());
            return;
        }
        
        try {
            String subject = "¡Pago Aprobado! Confirmación de Pedido #" + event.getOrderId();
            String htmlContent = buildOrderPaidTemplate(event);
            emailSenderClient.sendEmail(event.getGuestEmail(), subject, htmlContent);
        } catch (Exception e) {
            redisTemplate.delete(lockKey);
            log.error("No se pudo enviar el correo de pago de la orden {}. Llave liberada.", event.getOrderId());
            throw e;
        }
    }

    public void sendShippingConfirmation(OrderShippedEvent event) {
        if (event.getGuestEmail() == null || event.getGuestEmail().isBlank()) return;

        String lockKey = "lock:email:shipped:" + event.getOrderId();
        Boolean isFirstTime = redisTemplate.opsForValue().setIfAbsent(lockKey, "SENT", Duration.ofDays(7));
        
        if (Boolean.FALSE.equals(isFirstTime)) {
            log.info("El correo de Envío de la orden {} ya se envió. Ignorando.", event.getOrderId());
            return;
        }

        try {
            String subject = "¡Tu pedido #" + event.getOrderId() + " ya está en camino!";
            Context context = new Context();
            context.setVariable("orderId", event.getOrderId());
            context.setVariable("trackingNumber", event.getTrackingNumber());
            context.setVariable("estimatedDelivery", event.getEstimatedDelivery());
            context.setVariable("carrier", event.getCarrier());

            String htmlContent = templateEngine.process("shipping-confirmation-email", context);

            emailSenderClient.sendEmail(event.getGuestEmail(), subject, htmlContent);
        } catch (Exception e) {
            redisTemplate.delete(lockKey);
            log.error("No se pudo enviar el correo de shipping de la orden {}. Llave liberada.", event.getOrderId());
            throw e;
        }
    }

    private String buildOrderPaidTemplate(OrderPaidEvent event) {
        String formattedTotal = event.getTotal() != null ? String.format("%.2f", event.getTotal()) : "0.00";
        Context context = new Context();
        context.setVariable("orderId", event.getOrderId());
        context.setVariable("total", formattedTotal);
        context.setVariable("items", event.getItems());
        context.setVariable("createdAt", event.getCreatedAt());

        return templateEngine.process("order-paid-email", context);
    }
}