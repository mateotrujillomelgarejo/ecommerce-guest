package pe.takiq.ecommerce.notification_service.config;

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

    public static final String ORDER_EVENTS_EXCHANGE  = "ecommerce.events";
    public static final String ORDER_PAID_QUEUE       = "notification.order-paid.queue";
    public static final String ORDER_SHIPPED_QUEUE    = "notification.order-shipped.queue";
    public static final String USER_REGISTERED_QUEUE  = "notification.user-registered.queue"; // ✅ nuevo
    public static final String ROUTING_KEY_PAID       = "order.paid";
    public static final String ROUTING_KEY_SHIPPED    = "order.shipped";
    public static final String ROUTING_KEY_REGISTERED = "user.registered";                    // ✅ nuevo
    public static final String NOTIFICATION_DLQ       = "notification.dlq";
    public static final String NOTIFICATION_DLX       = "notification.dlx";

    @Bean
    public TopicExchange orderEventsExchange() {
        return new TopicExchange(ORDER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange notificationDlx() {
        return new DirectExchange(NOTIFICATION_DLX, true, false);
    }

    @Bean
    public Queue orderPaidQueue() {
        return QueueBuilder.durable(ORDER_PAID_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLQ)
                .build();
    }

    @Bean
    public Queue orderShippedQueue() {
        return QueueBuilder.durable(ORDER_SHIPPED_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLQ)
                .build();
    }
    
    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder.durable(USER_REGISTERED_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLQ)
                .build();
    }

    @Bean
    public Queue notificationDlq() {
        return QueueBuilder.durable(NOTIFICATION_DLQ).build();
    }

    @Bean
    public Binding bindOrderPaid(
            @Qualifier("orderPaidQueue") Queue orderPaidQueue,
            TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(orderPaidQueue)
                .to(orderEventsExchange)
                .with(ROUTING_KEY_PAID);
    }

    @Bean
    public Binding bindOrderShipped(
            @Qualifier("orderShippedQueue") Queue orderShippedQueue,
            TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(orderShippedQueue)
                .to(orderEventsExchange)
                .with(ROUTING_KEY_SHIPPED);
    }

    @Bean
    public Binding bindUserRegistered(
            @Qualifier("userRegisteredQueue") Queue userRegisteredQueue,
            TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(userRegisteredQueue)
                .to(orderEventsExchange)
                .with(ROUTING_KEY_REGISTERED);
    }

    @Bean
    public Binding bindDlq(
            @Qualifier("notificationDlq") Queue notificationDlq,
            DirectExchange notificationDlx) {
        return BindingBuilder.bind(notificationDlq)
                .to(notificationDlx)
                .with(NOTIFICATION_DLQ);
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
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}