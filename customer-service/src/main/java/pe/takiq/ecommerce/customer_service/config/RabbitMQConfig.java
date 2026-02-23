package pe.takiq.ecommerce.customer_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String GUEST_EVENTS_EXCHANGE = "guest.events.exchange";
    public static final String GUEST_CREATED_QUEUE = "guest.created.queue";
    public static final String ROUTING_KEY_CREATED = "guest.created";

    @Bean
    public TopicExchange guestExchange() {
        return new TopicExchange(GUEST_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue guestCreatedQueue() {
        return new Queue(GUEST_CREATED_QUEUE, true);
    }

    @Bean
    public Binding guestCreatedBinding() {
        return BindingBuilder.bind(guestCreatedQueue())
                .to(guestExchange())
                .with(ROUTING_KEY_CREATED);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(converter);
        return template;
    }
}