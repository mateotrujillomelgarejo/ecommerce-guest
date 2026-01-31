package pe.takiq.ecommerce.customer_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import pe.takiq.ecommerce.customer_service.model.Guest;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GuestCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${guest.cache.ttl-days:30}")
    private long ttlDays;

    private static final String KEY_PREFIX = "guest:";

    public Guest saveGuest(Guest guest) {
        String key = KEY_PREFIX + guest.getId();
        redisTemplate.opsForValue().set(key, guest, Duration.ofDays(ttlDays));
        return guest;
    }

    public Optional<Guest> getGuest(String guestId) {
        String key = KEY_PREFIX + guestId;
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj == null) return Optional.empty();
        return Optional.of(objectMapper.convertValue(obj, Guest.class));
    }

    public void deleteGuest(String guestId) {
        String key = KEY_PREFIX + guestId;
        redisTemplate.delete(key);
    }
}