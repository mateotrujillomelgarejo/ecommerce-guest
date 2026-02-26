package pe.takiq.ecommerce.pricing_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.takiq.ecommerce.pricing_service.events.ProductUpdatedEvent;

import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventListener {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String REDIS_HASH_KEY = "materialized_prices";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "pricing.product.updated.queue", durable = "true"),
            exchange = @Exchange(value = "ecommerce.events", type = "topic", ignoreDeclarationExceptions = "true"),
            key = "product.updated"
    ))
    public void onProductUpdated(ProductUpdatedEvent event) {
        redisTemplate.opsForHash().put(REDIS_HASH_KEY, event.getProductId(), event.getPrice().toString());
        log.info("Vista materializada actualizada en Pricing para producto {}: {}", event.getProductId(), event.getPrice());
    }
}