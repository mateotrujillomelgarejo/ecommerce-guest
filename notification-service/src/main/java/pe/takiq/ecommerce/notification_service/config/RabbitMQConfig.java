package pe.takiq.ecommerce.notification_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ────────────────────────────────────────────────
    // Exchange
    // ────────────────────────────────────────────────
    public static final String ORDER_EVENTS_EXCHANGE = "order.events.exchange";

    // ────────────────────────────────────────────────
    // Queues
    // ────────────────────────────────────────────────
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String NOTIFICATION_DLQ = "notification.dlq";

    // ────────────────────────────────────────────────
    // Routing Keys (emitidas por order-service)
    // ────────────────────────────────────────────────
    public static final String ROUTING_KEY_CREATED = "order.created";
    public static final String ROUTING_KEY_CONFIRMED = "order.confirmed";
    public static final String ROUTING_KEY_SHIPPED = "order.shipped";

    // ────────────────────────────────────────────────
    // Exchange
    // ────────────────────────────────────────────────
    @Bean
    public TopicExchange orderEventsExchange() {
        return new TopicExchange(ORDER_EVENTS_EXCHANGE, true, false);
    }

    // ────────────────────────────────────────────────
    // Queues
    // ────────────────────────────────────────────────
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .deadLetterExchange(ORDER_EVENTS_EXCHANGE)
                .deadLetterRoutingKey("notification.dlq.key")
                .build();
    }

    @Bean
    public Queue notificationDLQ() {
        return QueueBuilder.durable(NOTIFICATION_DLQ).build();
    }

    // ────────────────────────────────────────────────
    // Bindings
    // ────────────────────────────────────────────────
    @Bean
    public Binding bindOrderCreated() {
        return BindingBuilder.bind(notificationQueue())
                .to(orderEventsExchange())
                .with(ROUTING_KEY_CREATED);
    }

    @Bean
    public Binding bindOrderConfirmed() {
        return BindingBuilder.bind(notificationQueue())
                .to(orderEventsExchange())
                .with(ROUTING_KEY_CONFIRMED);
    }

    @Bean
    public Binding bindOrderShipped() {
        return BindingBuilder.bind(notificationQueue())
                .to(orderEventsExchange())
                .with(ROUTING_KEY_SHIPPED);
    }

    // ────────────────────────────────────────────────
    // JSON Converter + RabbitTemplate
    // ────────────────────────────────────────────────
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter
    ) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
