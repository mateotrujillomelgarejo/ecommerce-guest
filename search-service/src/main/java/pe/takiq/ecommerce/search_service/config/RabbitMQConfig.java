package pe.takiq.ecommerce.search_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.queue.product-updated}")
    private String productUpdatedQueueName;

    @Value("${rabbitmq.routing-key.product-updated}")
    private String productUpdatedRoutingKey;

    @Value("${rabbitmq.queue.stock-restocked:search.stock-restocked.queue}")
    private String stockRestockedQueueName;

    private static final String SEARCH_DLX = "search.dlx";
    private static final String SEARCH_DLQ = "search.product-updated.dlq";

    @Bean
    public TopicExchange ecommerceExchange() {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public DirectExchange searchDlx() {
        return new DirectExchange(SEARCH_DLX, true, false);
    }

    @Bean
    public Queue productUpdatedQueue() {
        return QueueBuilder.durable(productUpdatedQueueName)
                .withArgument("x-dead-letter-exchange", SEARCH_DLX)
                .withArgument("x-dead-letter-routing-key", SEARCH_DLQ)
                .build();
    }

    @Bean
    public Queue searchDlq() {
        return QueueBuilder.durable(SEARCH_DLQ).build();
    }

    @Bean
    public Queue stockRestockedQueue() {
        return QueueBuilder.durable(stockRestockedQueueName).build();
    }    

    @Bean
    public Binding productUpdatedBinding(
            Queue productUpdatedQueue,
            TopicExchange ecommerceExchange) {
        return BindingBuilder
                .bind(productUpdatedQueue)
                .to(ecommerceExchange)
                .with(productUpdatedRoutingKey);
    }

    @Bean
    public Binding searchDlqBinding(
            Queue searchDlq,
            DirectExchange searchDlx) {
        return BindingBuilder
                .bind(searchDlq)
                .to(searchDlx)
                .with(SEARCH_DLQ);
    }

    @Bean
    public Binding stockRestockedBinding(
            @Qualifier("stockRestockedQueue") Queue stockRestockedQueue,
            TopicExchange ecommerceExchange) {
        return BindingBuilder.bind(stockRestockedQueue)
                .to(ecommerceExchange)
                .with("stock.restocked");
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