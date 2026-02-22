package pe.takiq.ecommerce.notification_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.takiq.ecommerce.notification_service.events.OrderCreatedEvent;
import pe.takiq.ecommerce.notification_service.events.OrderShippedEvent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final RetryTemplate retryTemplate;
    
    private final StringRedisTemplate redisTemplate;

    @Value("${notification.from}")
    private String fromEmail;

    public void sendOrderCreatedNotification(OrderCreatedEvent event) {
        if (event.getGuestEmail() == null || event.getGuestEmail().isBlank()) return;

        String lockKey = "lock:email:created:" + event.getOrderId();
        Boolean isFirstTime = redisTemplate.opsForValue().setIfAbsent(lockKey, "SENT", Duration.ofDays(7));
        
        if (Boolean.FALSE.equals(isFirstTime)) {
            log.info("El correo de Confirmación de la orden {} ya se envió. Ignorando.", event.getOrderId());
            return;
        }
        
        String subject = "¡Tu pedido está confirmado! #" + event.getOrderId();
        String htmlContent = buildOrderCreatedTemplate(event);
        sendEmail(event.getGuestEmail(), subject, htmlContent);
    }

    public void sendShippingConfirmation(OrderShippedEvent event) {
        if (event.getGuestEmail() == null || event.getGuestEmail().isBlank()) return;

        String lockKey = "lock:email:shipped:" + event.getOrderId();
        Boolean isFirstTime = redisTemplate.opsForValue().setIfAbsent(lockKey, "SENT", Duration.ofDays(7));
        
        if (Boolean.FALSE.equals(isFirstTime)) {
            log.info("El correo de Envío de la orden {} ya se envió. Ignorando.", event.getOrderId());
            return;
        }

        String subject = "¡Tu pedido #" + event.getOrderId() + " ya está en camino!";
        Context context = new Context();
        context.setVariable("orderId", event.getOrderId());
        context.setVariable("trackingNumber", event.getTrackingNumber());
        context.setVariable("estimatedDelivery", event.getEstimatedDelivery());
        context.setVariable("carrier", event.getCarrier());

        String htmlContent = templateEngine.process("shipping-confirmation-email", context);
        sendEmail(event.getGuestEmail(), subject, htmlContent);
    }

    private String buildOrderCreatedTemplate(OrderCreatedEvent event) {
        String formattedTotal = event.getTotal() != null ? String.format("%.2f", event.getTotal()) : "0.00";
        Context context = new Context();
        context.setVariable("orderId", event.getOrderId());
        context.setVariable("total", formattedTotal);
        context.setVariable("items", event.getItems());
        context.setVariable("createdAt", event.getCreatedAt());

        return templateEngine.process("order-created-email", context);
    }

    private void sendEmail(String to, String subject, String htmlContent) {
        retryTemplate.execute(ctx -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);
                mailSender.send(message);
                log.info("Email enviado OK a: {}", to);
                return null;
            } catch (MessagingException e) {
                throw new RuntimeException("Fallo enviando email", e);
            }
        }, recovery -> {
            log.error("Fallo definitivo enviando email a {} → {}", to, recovery.getLastThrowable().getMessage());
            return null;
        });
    }
}