package pe.takiq.ecommerce.user_service.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token inválido: {}", e.getMessage());
            return false;
        }
    }

    public Claims getClaims(String token) {
        try {
            return Jwts.parser().verifyWith(getKey()).build()
                .parseSignedClaims(token).getPayload();
        } catch (JwtException e) {
            log.error("Error al extraer claims: {}", e.getMessage());
            return null;
        }
    }

    public String getUserId(String token) {
        Claims c = getClaims(token);
        return c != null ? c.getSubject() : null;
    }

    public String getUserType(String token) {
        Claims c = getClaims(token);
        return c != null ? c.get("userType", String.class) : null;
    }

    public String getSessionId(String token) {
        Claims c = getClaims(token);
        return c != null ? c.get("sessionId", String.class) : null;
    }

    @SuppressWarnings("unchecked")
    public Set<String> getRoles(String token) {
        Claims c = getClaims(token);
        if (c == null) return Set.of();
        String role = c.get("role", String.class);
        if (role != null) return Set.of(role);
        List<String> list = c.get("roles", List.class);
        return list != null ? new HashSet<>(list) : Set.of();
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
