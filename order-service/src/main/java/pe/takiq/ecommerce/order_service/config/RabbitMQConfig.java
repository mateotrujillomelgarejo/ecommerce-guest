package pe.takiq.ecommerce.order_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "ecommerce.events";
    public static final String QUEUE_PAYMENT_SUCCEEDED = "order.payment.succeeded.queue";
    public static final String QUEUE_INVENTORY_FAILED = "order.inventory.failed.queue";
    public static final String QUEUE_ORDER_SHIPPED = "order.shipped.queue";
    public static final String QUEUE_ORDER_DELIVERED = "order.delivered.queue";

    public static final String ROUTING_PAYMENT_SUCCEEDED = "payment.succeeded";
    public static final String ROUTING_INVENTORY_FAILED = "inventory.failed";
    public static final String ROUTING_ORDER_SHIPPED = "order.shipped";
    public static final String ROUTING_ORDER_DELIVERED = "order.delivered";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue paymentSucceededQueue() {
        return new Queue(QUEUE_PAYMENT_SUCCEEDED, true);
    }

    @Bean
    public Queue inventoryFailedQueue() {
        return new Queue(QUEUE_INVENTORY_FAILED, true);
    }

    @Bean
    public Queue orderShippedQueue() {
        return new Queue(QUEUE_ORDER_SHIPPED, true);
    }

    @Bean
    public Queue orderDeliveredQueue() {
        return new Queue(QUEUE_ORDER_DELIVERED, true);
    }

@Bean
public Binding paymentSucceededBinding(
        @Qualifier("paymentSucceededQueue") Queue paymentSucceededQueue,
        TopicExchange exchange
) {
    return BindingBuilder
            .bind(paymentSucceededQueue)
            .to(exchange)
            .with(ROUTING_PAYMENT_SUCCEEDED);
}

@Bean
public Binding inventoryFailedBinding(
        @Qualifier("inventoryFailedQueue") Queue inventoryFailedQueue,
        TopicExchange exchange
) {
    return BindingBuilder
            .bind(inventoryFailedQueue)
            .to(exchange)
            .with(ROUTING_INVENTORY_FAILED);
}

@Bean
public Binding orderShippedBinding(
        @Qualifier("orderShippedQueue") Queue orderShippedQueue,
        TopicExchange exchange
) {
    return BindingBuilder
            .bind(orderShippedQueue)
            .to(exchange)
            .with(ROUTING_ORDER_SHIPPED);
}

@Bean
public Binding orderDeliveredBinding(
        @Qualifier("orderDeliveredQueue") Queue orderDeliveredQueue,
        TopicExchange exchange
) {
    return BindingBuilder
            .bind(orderDeliveredQueue)
            .to(exchange)
            .with(ROUTING_ORDER_DELIVERED);
}


    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}