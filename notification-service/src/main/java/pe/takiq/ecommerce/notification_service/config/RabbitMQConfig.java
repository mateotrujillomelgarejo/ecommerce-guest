package pe.takiq.ecommerce.notification_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EVENTS_EXCHANGE = "ecommerce.events";

    public static final String ORDER_CREATED_QUEUE   = "notification.order-created.queue";
    public static final String ORDER_SHIPPED_QUEUE   = "notification.order-shipped.queue";;

    public static final String ROUTING_KEY_CREATED   = "order.created";
    public static final String ROUTING_KEY_SHIPPED   = "order.shipped";

    @Bean
    public TopicExchange orderEventsExchange() {
        return new TopicExchange(ORDER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(ORDER_CREATED_QUEUE).build();
    }

    @Bean
    public Queue orderShippedQueue() {
        return QueueBuilder.durable(ORDER_SHIPPED_QUEUE).build();
    }

    @Bean
    public Binding bindOrderCreated() {
        return BindingBuilder.bind(orderCreatedQueue())
                .to(orderEventsExchange())
                .with(ROUTING_KEY_CREATED);
    }

    @Bean
    public Binding bindOrderShipped() {
        return BindingBuilder.bind(orderShippedQueue())
                .to(orderEventsExchange())
                .with(ROUTING_KEY_SHIPPED);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
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