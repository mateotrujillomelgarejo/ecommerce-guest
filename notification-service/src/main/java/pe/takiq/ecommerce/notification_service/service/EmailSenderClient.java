package pe.takiq.ecommerce.notification_service.service;

import io.github.resilience4j.retry.annotation.Retry;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSenderClient {

    private final JavaMailSender mailSender;

    @Value("${notification.from}")
    private String fromEmail;

    @Retry(name = "emailSender", fallbackMethod = "sendEmailFallback")
    public void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email enviado OK a: {}", to);
        } catch (MessagingException e) {
            log.error("Fallo interno enviando email a {}: {}", to, e.getMessage());
            throw new RuntimeException("Fallo procesando MimeMessage", e);
        }
    }

    public void sendEmailFallback(String to, String subject, String htmlContent, Throwable t) {
        log.error("Fallo DEFINITIVO enviando email a {} tras agotar reintentos de Resilience4j. Causa: {}", to, t.getMessage());
        throw new RuntimeException("Fallo al enviar correo después de reintentos máximos", t);
    }
}