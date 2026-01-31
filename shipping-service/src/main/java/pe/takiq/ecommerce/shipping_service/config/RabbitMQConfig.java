package pe.takiq.ecommerce.shipping_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EVENTS_EXCHANGE = "order.events.exchange";
    public static final String SHIPPING_CREATED_QUEUE = "shipping.created.queue";
    public static final String ROUTING_KEY_CREATED = "order.created";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EVENTS_EXCHANGE);
    }

    @Bean
    public Queue shippingCreatedQueue() {
        return new Queue(SHIPPING_CREATED_QUEUE, true);
    }

    @Bean
    public Binding shippingCreatedBinding(
            Queue shippingCreatedQueue,
            TopicExchange orderExchange
    ) {
        return BindingBuilder.bind(shippingCreatedQueue)
                .to(orderExchange)
                .with(ROUTING_KEY_CREATED);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory cf,
            Jackson2JsonMessageConverter converter
    ) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(converter);
        return template;
    }
}
