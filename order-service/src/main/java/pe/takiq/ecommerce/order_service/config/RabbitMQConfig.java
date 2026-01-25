package pe.takiq.ecommerce.order_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EVENTS_EXCHANGE = "order.events.exchange";
    public static final String ORDER_CONFIRMED_QUEUE = "order.confirmed.queue";
    public static final String ROUTING_KEY_CONFIRMED = "order.confirmed";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderConfirmedQueue() {
        return new Queue(ORDER_CONFIRMED_QUEUE, true);
    }

    @Bean
    public Binding bindingConfirmed(Queue orderConfirmedQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderConfirmedQueue)
                .to(orderExchange)
                .with(ROUTING_KEY_CONFIRMED);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

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