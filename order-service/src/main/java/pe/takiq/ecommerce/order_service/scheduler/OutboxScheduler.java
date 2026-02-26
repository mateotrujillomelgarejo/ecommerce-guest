package pe.takiq.ecommerce.order_service.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pe.takiq.ecommerce.order_service.config.RabbitMQConfig;
import pe.takiq.ecommerce.order_service.event.OrderCancelledEvent;
import pe.takiq.ecommerce.order_service.event.OrderPaidEvent;

import pe.takiq.ecommerce.order_service.model.OutboxEvent;
import pe.takiq.ecommerce.order_service.repository.OutboxEventRepository;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxEventRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void processOutbox() {

        List<OutboxEvent> events = outboxRepository.findByProcessedFalse();

        for (OutboxEvent event : events) {
            try {

                switch (event.getEventType()) {

                    case "order.paid":
                        OrderPaidEvent paidPojo =
                                objectMapper.readValue(event.getPayload(), OrderPaidEvent.class);

                        rabbitTemplate.convertAndSend(
                                RabbitMQConfig.EXCHANGE,
                                event.getEventType(),
                                paidPojo
                        );
                        break;

                    case "order.cancelled":
                         OrderCancelledEvent cancelPojo =
                                 objectMapper.readValue(event.getPayload(), OrderCancelledEvent.class);
                    
                         rabbitTemplate.convertAndSend(
                                 RabbitMQConfig.EXCHANGE,
                                 event.getEventType(),
                                 cancelPojo
                         );
                         break;

                    default:
                        log.warn("Tipo de evento no reconocido: {}. Se omitirá el envío.",
                                event.getEventType());
                        continue;
                }

                event.setProcessed(true);
                outboxRepository.save(event);

                log.info("Outbox procesado exitosamente: {} para orden {}",
                        event.getEventType(),
                        event.getAggregateId());

            } catch (Exception e) {
                log.error("Error procesando evento outbox: {} de la orden {}",
                        event.getId(),
                        event.getAggregateId(),
                        e);
            }
        }
    }
}