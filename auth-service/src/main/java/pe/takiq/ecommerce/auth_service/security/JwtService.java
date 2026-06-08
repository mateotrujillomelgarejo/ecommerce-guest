package pe.takiq.ecommerce.auth_service.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import pe.takiq.ecommerce.auth_service.domain.AuthUser;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpMs;
    private final long refreshTokenExpMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpMs,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpMs = accessTokenExpMs;
        this.refreshTokenExpMs = refreshTokenExpMs;
    }

    /**
     * Access Token: contiene userId, role, permissions.
     * NO contiene nombre, email ni ningún dato de perfil.
     */
    public String generateAccessToken(AuthUser user) {
        Set<String> permissions = user.getPermissions().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())       // jti: identificador único del token
                .subject(user.getId().toString())       // sub = userId
                .claim("role", user.getRole().name())
                .claim("permissions", permissions)
                .claim("type", "access")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusMillis(accessTokenExpMs)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Refresh Token: solo lleva userId y type=refresh.
     * No lleva roles ni permissions (eso se re-calcula al refrescar).
     */
    public String generateRefreshToken(AuthUser user) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .claim("type", "refresh")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusMillis(refreshTokenExpMs)))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        return "access".equals(claims.get("type", String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get("type", String.class));
    }

    public long getAccessTokenExpMs() {
        return accessTokenExpMs;
    }

    public long getRefreshTokenExpMs() {
        return refreshTokenExpMs;
    }
}