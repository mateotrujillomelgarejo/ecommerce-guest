package pe.takiq.ecommerce.notification_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.takiq.ecommerce.notification_service.events.OrderPaidEvent;
import pe.takiq.ecommerce.notification_service.events.OrderShippedEvent;
import pe.takiq.ecommerce.notification_service.events.UserRegisteredEvent;

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

    // ─────────────────────────────────────────────────────────────────────────
    // ORDER PAID
    // ─────────────────────────────────────────────────────────────────────────

    public void sendOrderPaidNotification(OrderPaidEvent event) {
        if (event.getGuestEmail() == null || event.getGuestEmail().isBlank()) return;

        String lockKey = "lock:email:paid:" + event.getOrderId();
        Boolean isFirstTime = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "SENT", Duration.ofDays(7));

        if (Boolean.FALSE.equals(isFirstTime)) {
            log.info("Email de pago para orderId={} ya enviado. Ignorando duplicado.",
                    event.getOrderId());
            return;
        }

        try {
            String subject = "¡Pago Aprobado! Confirmación de Pedido #" + event.getOrderId();
            String htmlContent = buildOrderPaidTemplate(event);
            emailSenderClient.sendEmail(event.getGuestEmail(), subject, htmlContent);
            log.info("Email order.paid enviado → orderId={}", event.getOrderId());
        } catch (Exception e) {
            redisTemplate.delete(lockKey);
            log.error("No se pudo enviar email de pago para orderId={}. Llave liberada.",
                    event.getOrderId());
            throw e;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ORDER SHIPPED
    // ─────────────────────────────────────────────────────────────────────────

    public void sendShippingConfirmation(OrderShippedEvent event) {
        if (event.getGuestEmail() == null || event.getGuestEmail().isBlank()) return;

        String lockKey = "lock:email:shipped:" + event.getOrderId();
        Boolean isFirstTime = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "SENT", Duration.ofDays(7));

        if (Boolean.FALSE.equals(isFirstTime)) {
            log.info("Email de envío para orderId={} ya enviado. Ignorando.", event.getOrderId());
            return;
        }

        try {
            String subject = "¡Tu pedido #" + event.getOrderId() + " ya está en camino!";
            Context context = new Context();
            context.setVariable("orderId", event.getOrderId());
            context.setVariable("trackingNumber", event.getTrackingNumber());
            context.setVariable("estimatedDelivery", event.getEstimatedDelivery());
            context.setVariable("carrier", event.getCarrier());
            context.setVariable("message", event.getMessage());

            String htmlContent = templateEngine.process("shipping-confirmation-email", context);
            emailSenderClient.sendEmail(event.getGuestEmail(), subject, htmlContent);
            log.info("Email order.shipped enviado → orderId={}", event.getOrderId());
        } catch (Exception e) {
            redisTemplate.delete(lockKey);
            log.error("No se pudo enviar email de envío para orderId={}. Llave liberada.",
                    event.getOrderId());
            throw e;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // USER REGISTERED — email de bienvenida 
    // ─────────────────────────────────────────────────────────────────────────

    public void sendWelcomeEmail(UserRegisteredEvent event) {
        if (event.getEmail() == null || event.getEmail().isBlank()) return;

        String lockKey = "lock:email:welcome:" + event.getUserId();
        Boolean isFirstTime = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "SENT", Duration.ofDays(7));

        if (Boolean.FALSE.equals(isFirstTime)) {
            log.info("Email de bienvenida para userId={} ya enviado. Ignorando.",
                    event.getUserId());
            return;
        }

        try {
            String subject = "¡Bienvenido a nuestra tienda!";
            Context context = new Context();
            context.setVariable("email", event.getEmail());
            context.setVariable("userId", event.getUserId());

            String htmlContent = templateEngine.process("welcome-email", context);
            emailSenderClient.sendEmail(event.getEmail(), subject, htmlContent);
            log.info("Email de bienvenida enviado → userId={}", event.getUserId());
        } catch (Exception e) {
            redisTemplate.delete(lockKey);
            log.error("No se pudo enviar email de bienvenida para userId={}. Llave liberada.",
                    event.getUserId());
            throw e;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private String buildOrderPaidTemplate(OrderPaidEvent event) {
        Context context = new Context();
        context.setVariable("orderId",   event.getOrderId());
        context.setVariable("items",     event.getItems());
        context.setVariable("createdAt", event.getCreatedAt());

        context.setVariable("subtotal",     format(event.getSubtotal()));
        context.setVariable("discount",     format(event.getDiscount()));
        context.setVariable("tax",          format(event.getTax()));
        context.setVariable("shippingCost", format(event.getShippingCost()));
        context.setVariable("total",        format(event.getTotal()));

        return templateEngine.process("order-paid-email", context);
    }

    private String format(java.math.BigDecimal value) {
        return value != null ? String.format("%.2f", value) : "0.00";
    }
}