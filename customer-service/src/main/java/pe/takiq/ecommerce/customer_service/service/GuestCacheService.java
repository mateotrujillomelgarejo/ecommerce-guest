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

    private static final String ID_PREFIX = "guest:id:";
    private static final String SESSION_PREFIX = "guest:session:";
    private static final String EMAIL_PREFIX = "guest:email:";

    public Guest saveGuest(Guest guest) {
        Duration ttl = Duration.ofDays(ttlDays);
        
        // Guardar por ID
        redisTemplate.opsForValue().set(ID_PREFIX + guest.getId(), guest, ttl);
        
        // Guardar índice por SessionId
        if (guest.getSessionId() != null) {
            redisTemplate.opsForValue().set(SESSION_PREFIX + guest.getSessionId(), guest, ttl);
        }
        
        // Guardar índice por Email
        if (guest.getEmail() != null) {
            redisTemplate.opsForValue().set(EMAIL_PREFIX + guest.getEmail(), guest, ttl);
        }
        
        return guest;
    }

    public Optional<Guest> getGuestById(String guestId) {
        return getFromCache(ID_PREFIX + guestId);
    }

    public Optional<Guest> getGuestBySessionId(String sessionId) {
        return getFromCache(SESSION_PREFIX + sessionId);
    }

    public Optional<Guest> getGuestByEmail(String email) {
        return getFromCache(EMAIL_PREFIX + email);
    }

    public void deleteGuest(Guest guest) {
        redisTemplate.delete(ID_PREFIX + guest.getId());
        if (guest.getSessionId() != null) redisTemplate.delete(SESSION_PREFIX + guest.getSessionId());
        if (guest.getEmail() != null) redisTemplate.delete(EMAIL_PREFIX + guest.getEmail());
    }

    private Optional<Guest> getFromCache(String key) {
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj == null) return Optional.empty();
        
        // Optimización: si ya es del tipo Guest, lo casteamos, si no, usamos objectMapper (por seguridad con Maps de Jackson)
        if (obj instanceof Guest) {
            return Optional.of((Guest) obj);
        }
        return Optional.of(objectMapper.convertValue(obj, Guest.class));
    }
}