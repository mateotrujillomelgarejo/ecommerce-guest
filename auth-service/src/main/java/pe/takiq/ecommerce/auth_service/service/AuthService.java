package pe.takiq.ecommerce.auth_service.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.takiq.ecommerce.auth_service.domain.AuthUser;
import pe.takiq.ecommerce.auth_service.domain.Permission;
import pe.takiq.ecommerce.auth_service.domain.Role;
import pe.takiq.ecommerce.auth_service.dto.AuthResponse;
import pe.takiq.ecommerce.auth_service.dto.LoginRequest;
import pe.takiq.ecommerce.auth_service.dto.RefreshRequest;
import pe.takiq.ecommerce.auth_service.dto.RegisterRequest;
import pe.takiq.ecommerce.auth_service.dto.TokenValidationResponse;
import pe.takiq.ecommerce.auth_service.event.UserRegisteredEvent;
import pe.takiq.ecommerce.auth_service.exception.AuthException;
import pe.takiq.ecommerce.auth_service.repository.AuthUserRepository;
import pe.takiq.ecommerce.auth_service.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import pe.takiq.ecommerce.auth_service.domain.AuthOutboxEvent;
import pe.takiq.ecommerce.auth_service.repository.AuthOutboxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthUserRepository authUserRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final AuthOutboxEventRepository authOutboxEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.user-registered}")
    private String userRegisteredRoutingKey;

    @Value("${auth.refresh-token.ttl-seconds}")
    private long refreshTokenTtlSeconds;

    @Value("${auth.revoked-token.ttl-seconds}")
    private long revokedTokenTtlSeconds;

    // ─────────────────────────────────────────────────────────────────────────
    // REGISTER
    // ─────────────────────────────────────────────────────────────────────────

@Transactional
public AuthResponse register(RegisterRequest request) {
    if (authUserRepository.existsByEmail(request.getEmail())) {
        throw AuthException.emailAlreadyExists();
    }

    Role role = parseRole(request.getRole());
    Set<Permission> permissions = defaultPermissionsFor(role);

    AuthUser user = AuthUser.builder()
            .id(UUID.randomUUID())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role(role)
            .permissions(permissions)
            .active(true)
            .createdAt(Instant.now())
            .build();

    authUserRepository.save(user);

    try {
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(user.getId().toString())
                .email(user.getEmail())
                .role(user.getRole().name())
                .registeredAt(user.getCreatedAt())
                .build();

        AuthOutboxEvent outboxEvent = AuthOutboxEvent.builder()
                .eventType("user.registered")
                .payload(objectMapper.writeValueAsString(event))
                .build();

        authOutboxEventRepository.save(outboxEvent);
        log.info("Evento user.registered guardado en Outbox: userId={}", user.getId());

    } catch (Exception e) {
        log.error("CRÍTICO: No se pudo guardar el evento Outbox para userId={}: {}",
                user.getId(), e.getMessage());
    }

    log.info("Nuevo usuario registrado: userId={} role={}", user.getId(), role);
    return buildAuthResponse(user);
}

    // ─────────────────────────────────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest request) {
        AuthUser user = authUserRepository.findByEmail(request.getEmail())
                .orElseThrow(AuthException::invalidCredentials);

        if (!user.isActive()) {
            throw AuthException.userDisabled();
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw AuthException.invalidCredentials();
        }

        user.setLastLoginAt(Instant.now());
        authUserRepository.save(user);

        AuthResponse response = buildAuthResponse(user);

        // Guarda el refresh token en Redis para poder validarlo y revocarlo
        storeRefreshToken(user.getId().toString(), response.getRefreshToken());

        log.info("Login exitoso: userId={}", user.getId());
        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REFRESH
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtService.parseToken(request.getRefreshToken());
        } catch (JwtException e) {
            throw AuthException.invalidRefreshToken();
        }

        if (!jwtService.isRefreshToken(claims)) {
            throw AuthException.invalidRefreshToken();
        }

        String userId = claims.getSubject();

        // Verificar que el refresh token en Redis coincida (single-session por usuario)
        String storedToken = redisTemplate.opsForValue().get("refresh:" + userId);
        if (!request.getRefreshToken().equals(storedToken)) {
            throw AuthException.refreshTokenMismatch();
        }

        AuthUser user = authUserRepository.findById(UUID.fromString(userId))
                .orElseThrow(AuthException::userNotFound);

        if (!user.isActive()) {
            throw AuthException.userDisabled();
        }

        // Rota el refresh token (invalida el anterior automáticamente)
        AuthResponse response = buildAuthResponse(user);
        storeRefreshToken(userId, response.getRefreshToken());

        log.info("Token refrescado: userId={}", userId);
        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGOUT
    // ─────────────────────────────────────────────────────────────────────────

    public void logout(String accessToken) {
        try {
            Claims claims = jwtService.parseToken(accessToken);
            String jti = claims.getId();
            String userId = claims.getSubject();

            // Agrega el jti a la lista negra con TTL igual al tiempo restante del token
            long expiresAt = claims.getExpiration().getTime();
            long now = System.currentTimeMillis();
            long ttlMs = Math.max(expiresAt - now, 0);

            if (ttlMs > 0) {
                redisTemplate.opsForValue().set(
                        "revoked:" + jti, "1", ttlMs, TimeUnit.MILLISECONDS);
            }

            // Elimina el refresh token de Redis
            redisTemplate.delete("refresh:" + userId);

            log.info("Logout exitoso: userId={}", userId);
        } catch (JwtException e) {
            // Si el token ya expiró, el logout es un no-op silencioso
            log.debug("Logout con token inválido/expirado, ignorado");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VALIDATE (para API Gateway u otros servicios internos)
    // ─────────────────────────────────────────────────────────────────────────

    public TokenValidationResponse validate(String token) {
        try {
            Claims claims = jwtService.parseToken(token);

            if (!jwtService.isAccessToken(claims)) {
                return TokenValidationResponse.builder()
                        .valid(false)
                        .reason("Tipo de token incorrecto")
                        .build();
            }

            // Verificar lista negra
            String jti = claims.getId();
            if (Boolean.TRUE.equals(redisTemplate.hasKey("revoked:" + jti))) {
                return TokenValidationResponse.builder()
                        .valid(false)
                        .reason("Token revocado")
                        .build();
            }

            @SuppressWarnings("unchecked")
            java.util.List<String> permissions = (java.util.List<String>) claims.get("permissions");

            return TokenValidationResponse.builder()
                    .valid(true)
                    .userId(claims.getSubject())
                    .role(claims.get("role", String.class))
                    .permissions(Set.copyOf(permissions))
                    .build();

        } catch (JwtException e) {
            return TokenValidationResponse.builder()
                    .valid(false)
                    .reason("Token inválido o expirado")
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS PRIVADOS
    // ─────────────────────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(AuthUser user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        Set<String> permissions = user.getPermissions().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpMs() / 1000)
                .userId(user.getId().toString())
                .role(user.getRole().name())
                .permissions(permissions)
                .build();
    }

    private void storeRefreshToken(String userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                "refresh:" + userId,
                refreshToken,
                refreshTokenTtlSeconds,
                TimeUnit.SECONDS
        );
    }

    private Role parseRole(String roleStr) {
        if (roleStr == null) return Role.CUSTOMER;
        try {
            return Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Role.CUSTOMER;
        }
    }

    private Set<Permission> defaultPermissionsFor(Role role) {
        return switch (role) {
            case ADMIN -> Set.of(Permission.values());
            case CUSTOMER -> Set.of(
                    Permission.READ_PRODUCTS,
                    Permission.WRITE_ORDERS,
                    Permission.READ_OWN_ORDERS
            );
            case GUEST -> Set.of(Permission.READ_PRODUCTS);
        };
    }
}