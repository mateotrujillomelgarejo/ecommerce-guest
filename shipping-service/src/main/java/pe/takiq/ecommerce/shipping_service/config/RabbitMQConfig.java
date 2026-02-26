package pe.takiq.ecommerce.shipping_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EVENTS_EXCHANGE = "ecommerce.events";

    public static final String ORDER_CREATED_QUEUE = "shipping.order-created.queue";
    public static final String ORDER_CREATED_DLQ   = "shipping.order-created.dlq";

    public static final String ROUTING_KEY_CREATED = "order.paid";
    public static final String ROUTING_KEY_SHIPPED = "order.shipped";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(ORDER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_EVENTS_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq.shipping.created")
                .build();
    }

    @Bean
    public Queue orderCreatedDlq() {
        return QueueBuilder.durable(ORDER_CREATED_DLQ).build();
    }

@Bean
public Binding createdBinding(
        @Qualifier("orderCreatedQueue") Queue orderCreatedQueue,
        TopicExchange exchange
) {
    return BindingBuilder.bind(orderCreatedQueue)
            .to(exchange)
            .with(ROUTING_KEY_CREATED);
}

@Bean
public Binding dlqBinding(
        @Qualifier("orderCreatedDlq") Queue orderCreatedDlq,
        TopicExchange exchange
) {
    return BindingBuilder.bind(orderCreatedDlq)
            .to(exchange)
            .with("dlq.shipping.created");
}


@Bean
public Jackson2JsonMessageConverter jsonMessageConverter() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule()); // Soporte para LocalDateTime
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Formato ISO-8601 (String)

    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); 

    Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
    converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
    return converter;
}

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory cf,
            Jackson2JsonMessageConverter conv) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(conv);
        return t;
    }
}
