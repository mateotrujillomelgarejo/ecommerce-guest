package pe.takiq.ecommerce.api_gateway.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AuthenticationFilter
        extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final WebClient webClient;
    private final Cache<String, TokenValidationResponse> tokenCache;

    @Value("${auth.validate.timeout-seconds:3}")
    private long timeoutSeconds;

    public AuthenticationFilter(
            @Value("${auth.service.url}") String authServiceUrl,
            @Value("${auth.validate.cache-ttl-seconds:30}") long cacheTtlSeconds) {

        super(Config.class);

        this.webClient = WebClient.builder()
                .baseUrl(authServiceUrl)
                .build();

        this.tokenCache = Caffeine.newBuilder()
                .expireAfterWrite(cacheTtlSeconds, TimeUnit.SECONDS)
                .maximumSize(10_000)
                .build();
    }

    private static final List<String> PUBLIC_PATHS = List.of(
        "/users/guests",
        "/users/session/",
        "/users/email/"
    );

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            boolean isPublic = PUBLIC_PATHS.stream().anyMatch(path::startsWith);
            if (isPublic) {
                return chain.filter(exchange);
            }

            String authHeader = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.debug("Request sin token a ruta protegida: {}",
                        exchange.getRequest().getURI().getPath());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = authHeader.substring(7);

            TokenValidationResponse cached = tokenCache.getIfPresent(token);
            if (cached != null) {
                if (!cached.isValid()) {
                    log.debug("Token inválido (cacheado): {}", cached.getReason());
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
                return chain.filter(buildMutatedExchange(exchange, cached));
            }

            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/auth/validate")
                            .queryParam("token", token)
                            .build())
                    .retrieve()
                    .bodyToMono(TokenValidationResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .flatMap(validation -> {
                        // Guardar en caché (válido o inválido)
                        tokenCache.put(token, validation);

                        if (!validation.isValid()) {
                            log.debug("Token rechazado por Auth Service: {}", validation.getReason());
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        }

                        log.debug("Token válido: userId={}, role={}",
                                validation.getUserId(), validation.getRole());
                        return chain.filter(buildMutatedExchange(exchange, validation));
                    })
                    .onErrorResume(ex -> {
                        log.error("Auth Service no disponible o timeout: {}", ex.getMessage());
                        // Si Auth Service cae, no podemos validar — rechazamos
                        // Para endpoints críticos (pagos, órdenes) esto es correcto.
                        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                        return exchange.getResponse().setComplete();
                    });
        };
    }

    private ServerWebExchange buildMutatedExchange(
            ServerWebExchange exchange,
            TokenValidationResponse validation) {

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", validation.getUserId())
                .header("X-User-Role", validation.getRole())
                .header("X-User-Permissions",
                        String.join(",", validation.getPermissions()))
                // Eliminar el Authorization original para que los servicios internos
                // no dependan del JWT — solo de los headers propagados
                .headers(headers -> headers.remove(HttpHeaders.AUTHORIZATION))
                .build();

        return exchange.mutate().request(mutatedRequest).build();
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TokenValidationResponse {
        private boolean valid;
        private String userId;
        private String role;
        private List<String> permissions;
        private String reason;
    }

    public static class Config {
        // Sin configuración por ruta — el filtro se comporta igual en todas las rutas
    }
}