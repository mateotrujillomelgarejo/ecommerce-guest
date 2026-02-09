package pe.takiq.ecommerce.inventory_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;


import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EVENTS_EXCHANGE = "order.events.exchange";

    public static final String ORDER_CREATED_QUEUE = "inventory.order-created.queue";
    public static final String ORDER_CREATED_DLQ   = "inventory.order-created.dlq";

    public static final String ROUTING_KEY_CREATED = "order.created";
    public static final String INVENTORY_FAILED_ROUTING_KEY = "inventory.failed";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(ORDER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_EVENTS_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq.order-created")
                .build();
    }

    @Bean
    public Queue orderCreatedDlq() {
        return QueueBuilder.durable(ORDER_CREATED_DLQ).build();
    }

    @Bean
    public Binding orderCreatedBinding(
        @Qualifier("orderCreatedQueue") Queue orderCreatedQueue,
        TopicExchange exchange) {

        return BindingBuilder.bind(orderCreatedQueue)
            .to(exchange)
            .with(ROUTING_KEY_CREATED);
    }


    @Bean
    public Binding dlqBinding(
        @Qualifier("orderCreatedDlq") Queue orderCreatedDlq,
        TopicExchange exchange) {

        return BindingBuilder.bind(orderCreatedDlq)
            .to(exchange)
            .with("dlq.order-created");
    }


    @Bean
    public Jackson2JsonMessageConverter jacksonConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
