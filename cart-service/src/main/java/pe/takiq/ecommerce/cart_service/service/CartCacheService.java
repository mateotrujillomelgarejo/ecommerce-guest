package pe.takiq.ecommerce.cart_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import pe.takiq.ecommerce.cart_service.model.Cart;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${cart.cache.ttl-days:30}")
    private long ttlDays;

    private static final String KEY_PREFIX = "cart:";

    public Cart saveCart(Cart cart) {
        String key = KEY_PREFIX + cart.getId();
        redisTemplate.opsForValue().set(key, cart, Duration.ofDays(ttlDays));
        return cart;
    }

    public Optional<Cart> getCart(String cartId) {
        String key = KEY_PREFIX + cartId;
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj == null) return Optional.empty();
        return Optional.of(objectMapper.convertValue(obj, Cart.class));
    }

    public void deleteCart(String cartId) {
        String key = KEY_PREFIX + cartId;
        redisTemplate.delete(key);
    }
}