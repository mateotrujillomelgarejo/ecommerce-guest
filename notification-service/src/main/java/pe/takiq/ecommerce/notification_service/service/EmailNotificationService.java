package pe.takiq.ecommerce.notification_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import pe.takiq.ecommerce.events.OrderCreatedEvent;
import pe.takiq.ecommerce.events.OrderConfirmedEvent;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final RetryTemplate retryTemplate;

    @Value("${notification.from}")
    private String fromEmail;

    public void sendOrderCreatedNotification(OrderCreatedEvent event) {
        String subject = "¡Recibimos tu pedido! #" + event.getOrderId();
        String htmlContent = buildTemplate("order-created-email", event);
        sendEmail(event.getGuestEmail(), subject, htmlContent);
    }

    public void sendOrderConfirmedNotification(OrderConfirmedEvent event) {
        String subject = "¡Pago confirmado! Tu pedido #" + event.getOrderId();
        String htmlContent = buildTemplate("order-confirmed-email", event);
        sendEmail(event.getGuestEmail(), subject, htmlContent);
    }

    private String buildTemplate(String templateName, Object event) {
        Context context = new Context();
        
        if (event instanceof OrderCreatedEvent) {
            OrderCreatedEvent e = (OrderCreatedEvent) event;
            context.setVariable("orderId", e.getOrderId());
            context.setVariable("total", String.format("%.2f", e.getTotalAmount()));
            context.setVariable("items", e.getItems());
            context.setVariable("createdAt", e.getCreatedAt());
        } else if (event instanceof OrderConfirmedEvent) {
            OrderConfirmedEvent e = (OrderConfirmedEvent) event;
            context.setVariable("orderId", e.getOrderId());
            context.setVariable("total", String.format("%.2f", e.getTotalAmount()));
            context.setVariable("items", e.getItems());
            context.setVariable("paymentId", e.getPaymentId());
            context.setVariable("confirmedAt", e.getConfirmedAt());
        }
        
        return templateEngine.process(templateName, context);
    }

private void sendEmail(String to, String subject, String htmlContent) {
    retryTemplate.execute(context -> {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email enviado exitosamente a: {}", to);

            return null;

        } catch (MessagingException e) {
            throw new RuntimeException("Error enviando email", e);
        }
    }, context -> {
        Throwable error = context.getLastThrowable();
        log.error(
                "Fallo definitivo después de reintentos enviando email a {}",
                to,
                error
        );
        return null;
    });
}

public void sendOrderCreatedNotification(Object event) {
        log.info("Email enviado: orden creada");
    }

    public void sendOrderConfirmedNotification(Object event) {
        log.info("Email enviado: orden confirmada");
    }

    public void sendShippingConfirmation(String orderId, String trackingNumber) {
        log.info(
            "Email enviado: orden {} enviada. Tracking: {}",
            orderId,
            trackingNumber
        );
    }
}