package pe.takiq.ecommerce.inventory_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EVENTS_EXCHANGE = "order.events.exchange";

    public static final String ORDER_CREATED_QUEUE = "order.created.queue";
    public static final String ORDER_CREATED_DLQ = "order.created.dlq";
    public static final String ROUTING_KEY_CREATED = "order.created";

    public static final String ORDER_CONFIRMED_QUEUE = "order.confirmed.queue";
    public static final String ORDER_CONFIRMED_DLQ = "order.confirmed.dlq";
    public static final String ROUTING_KEY_CONFIRMED = "order.confirmed";

    /* ===================== EXCHANGE ===================== */

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EVENTS_EXCHANGE, true, false);
    }

    /* ===================== QUEUES ===================== */

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(ORDER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_EVENTS_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq.created")
                .build();
    }

    @Bean
    public Queue orderConfirmedQueue() {
        return QueueBuilder.durable(ORDER_CONFIRMED_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_EVENTS_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq.confirmed")
                .build();
    }

    /* ===================== DLQs ===================== */

    @Bean
    public Queue orderCreatedDlq() {
        return new Queue(ORDER_CREATED_DLQ, true);
    }

    @Bean
    public Queue orderConfirmedDlq() {
        return new Queue(ORDER_CONFIRMED_DLQ, true);
    }

    /* ===================== BINDINGS ===================== */

    @Bean
    public Binding orderCreatedBinding(
            @Qualifier("orderCreatedQueue") Queue orderCreatedQueue,
            TopicExchange orderExchange) {

        return BindingBuilder
                .bind(orderCreatedQueue)
                .to(orderExchange)
                .with(ROUTING_KEY_CREATED);
    }

    @Bean
    public Binding orderConfirmedBinding(
            @Qualifier("orderConfirmedQueue") Queue orderConfirmedQueue,
            TopicExchange orderExchange) {

        return BindingBuilder
                .bind(orderConfirmedQueue)
                .to(orderExchange)
                .with(ROUTING_KEY_CONFIRMED);
    }

    @Bean
    public Binding dlqCreatedBinding(
            @Qualifier("orderCreatedDlq") Queue orderCreatedDlq,
            TopicExchange orderExchange) {

        return BindingBuilder
                .bind(orderCreatedDlq)
                .to(orderExchange)
                .with("dlq.created");
    }

    @Bean
    public Binding dlqConfirmedBinding(
            @Qualifier("orderConfirmedDlq") Queue orderConfirmedDlq,
            TopicExchange orderExchange) {

        return BindingBuilder
                .bind(orderConfirmedDlq)
                .to(orderExchange)
                .with("dlq.confirmed");
    }

    /* ===================== JSON ===================== */

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /* ===================== RABBIT TEMPLATE ===================== */

    @Primary
    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter) {

        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
