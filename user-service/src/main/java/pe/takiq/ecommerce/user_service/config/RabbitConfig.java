package pe.takiq.ecommerce.user_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${rabbitmq.exchange}")
    private String exchange;

    // ✅ Renombrado: era "userRegisteredQueue" — mismo nombre que el @Bean, causaba ambigüedad
    @Value("${rabbitmq.queue.user-registered}")
    private String userRegisteredQueueName;

    @Value("${rabbitmq.routing-key.user-registered}")
    private String userRegisteredKey;

    @Bean
    public TopicExchange ecommerceExchange() {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder.durable(userRegisteredQueueName).build();
    }

    @Bean
    public Binding userRegisteredBinding(Queue userRegisteredQueue, TopicExchange ecommerceExchange) {
        return BindingBuilder.bind(userRegisteredQueue)
                .to(ecommerceExchange)
                .with(userRegisteredKey);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}