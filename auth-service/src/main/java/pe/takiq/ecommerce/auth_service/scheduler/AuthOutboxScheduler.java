package pe.takiq.ecommerce.auth_service.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pe.takiq.ecommerce.auth_service.domain.AuthOutboxEvent;
import pe.takiq.ecommerce.auth_service.event.UserRegisteredEvent;
import pe.takiq.ecommerce.auth_service.repository.AuthOutboxEventRepository;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthOutboxScheduler {

    private final AuthOutboxEventRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Scheduled(fixedDelay = 3000)
    public void publishPendingEvents() {
        List<AuthOutboxEvent> pending = outboxRepository.findByProcessedFalse();

        for (AuthOutboxEvent event : pending) {
            try {
                Object payload = deserializePayload(event);
                if (payload == null) {
                    log.warn("No se pudo deserializar el evento outbox id={}", event.getId());
                    continue;
                }

                rabbitTemplate.convertAndSend(exchange, event.getEventType(), payload);

                event.setProcessed(true);
                event.setProcessedAt(Instant.now());
                outboxRepository.save(event);

                log.info("Evento Outbox publicado: id={}, eventType={}",
                        event.getId(), event.getEventType());

            } catch (Exception e) {
                log.error("Error publicando evento Outbox id={}: {}",
                        event.getId(), e.getMessage());
                // No marca como processed — se reintentará en 3 segundos
            }
        }
    }

    private Object deserializePayload(AuthOutboxEvent event) {
        try {
            return switch (event.getEventType()) {
                case "user.registered" ->
                        objectMapper.readValue(event.getPayload(), UserRegisteredEvent.class);
                default -> {
                    log.warn("Tipo de evento desconocido: {}", event.getEventType());
                    yield null;
                }
            };
        } catch (Exception e) {
            log.error("Error deserializando payload del evento {}: {}", event.getId(), e.getMessage());
            return null;
        }
    }
}