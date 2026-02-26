package pe.takiq.ecommerce.payment_service.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.takiq.ecommerce.payment_service.events.PaymentSucceededEvent;
import pe.takiq.ecommerce.payment_service.model.PaymentOutboxEvent;
import pe.takiq.ecommerce.payment_service.repository.PaymentOutboxRepository;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutboxScheduler {

    private final PaymentOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void processOutbox() {
        List<PaymentOutboxEvent> events = outboxRepository.findByProcessedFalse();
        for (PaymentOutboxEvent event : events) {
            try {
                if ("payment.succeeded".equals(event.getEventType())) {
                    PaymentSucceededEvent payload = objectMapper.readValue(event.getPayload(), PaymentSucceededEvent.class);
                    rabbitTemplate.convertAndSend("ecommerce.events", event.getEventType(), payload);
                }

                event.setProcessed(true);
                outboxRepository.save(event);
                log.info("Payment Outbox procesado exitosamente: {}", event.getId());
            } catch (Exception e) {
                log.error("Error procesando Payment Outbox {}", event.getId(), e);
            }
        }
    }
}