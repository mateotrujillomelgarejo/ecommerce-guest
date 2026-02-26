package pe.takiq.ecommerce.inventory_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EVENTS_EXCHANGE = "ecommerce.events";

    // Renombrado para mayor claridad
    public static final String ORDER_PAID_QUEUE = "inventory.order-paid.queue";
    public static final String ORDER_CANCELLED_QUEUE = "inventory.order-cancelled.queue"; // NUEVO
    public static final String INVENTORY_DLQ = "inventory.dlq";

    public static final String ROUTING_KEY_PAID = "order.paid";
    public static final String ROUTING_KEY_CANCELLED = "order.cancelled"; // NUEVO
    public static final String INVENTORY_FAILED_ROUTING_KEY = "inventory.failed";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderPaidQueue() {
        return QueueBuilder.durable(ORDER_PAID_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_EVENTS_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq.inventory")
                .build();
    }

    @Bean
    public Queue orderCancelledQueue() {
        return QueueBuilder.durable(ORDER_CANCELLED_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_EVENTS_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq.inventory")
                .build();
    }

    @Bean
    public Queue inventoryDlq() {
        return QueueBuilder.durable(INVENTORY_DLQ).build();
    }

    @Bean
    public Binding orderPaidBinding(@Qualifier("orderPaidQueue") Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY_PAID);
    }

    @Bean
    public Binding orderCancelledBinding(@Qualifier("orderCancelledQueue") Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY_CANCELLED);
    }

    @Bean
    public Binding dlqBinding(@Qualifier("inventoryDlq") Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("dlq.inventory");
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); 
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}