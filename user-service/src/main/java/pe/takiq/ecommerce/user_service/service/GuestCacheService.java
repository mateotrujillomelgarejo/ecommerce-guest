package pe.takiq.ecommerce.user_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import pe.takiq.ecommerce.user_service.entity.User;

import java.time.Duration;
import java.util.Optional;

/**
 * Caché Redis para perfiles GUEST.
 * Doble indexación: por userId y por sessionId (punteros),
 * sin duplicar el objeto completo. Mismo patrón que Customer Service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuestCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${guest.cache.ttl-days:30}")
    private long ttlDays;

    private static final String ID_PREFIX      = "user:guest:id:";
    private static final String SESSION_PREFIX = "user:guest:session:";
    private static final String EMAIL_PREFIX   = "user:guest:email:";

    public void saveGuest(User guest) {
        Duration ttl = Duration.ofDays(ttlDays);
        redisTemplate.opsForValue().set(ID_PREFIX + guest.getId(), guest, ttl);
        if (guest.getSessionId() != null) {
            redisTemplate.opsForValue().set(SESSION_PREFIX + guest.getSessionId(),
                guest.getId().toString(), ttl);
        }
        if (guest.getEmail() != null) {
            redisTemplate.opsForValue().set(EMAIL_PREFIX + guest.getEmail(),
                guest.getId().toString(), ttl);
        }
    }

    public Optional<User> getBySessionId(String sessionId) {
        return getIdByRef(SESSION_PREFIX + sessionId).flatMap(this::getById);
    }

    public Optional<User> getByEmail(String email) {
        return getIdByRef(EMAIL_PREFIX + email).flatMap(this::getById);
    }

    public Optional<User> getById(String userId) {
        Object obj = redisTemplate.opsForValue().get(ID_PREFIX + userId);
        if (obj == null) return Optional.empty();
        if (obj instanceof User) return Optional.of((User) obj);
        return Optional.of(objectMapper.convertValue(obj, User.class));
    }

    public void evict(User guest) {
        redisTemplate.delete(ID_PREFIX + guest.getId());
        if (guest.getSessionId() != null) redisTemplate.delete(SESSION_PREFIX + guest.getSessionId());
        if (guest.getEmail() != null)     redisTemplate.delete(EMAIL_PREFIX + guest.getEmail());
    }

    private Optional<String> getIdByRef(String refKey) {
        Object id = redisTemplate.opsForValue().get(refKey);
        return id == null ? Optional.empty() : Optional.of(id.toString());
    }
}
