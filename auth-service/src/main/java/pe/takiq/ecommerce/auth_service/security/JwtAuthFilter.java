package pe.takiq.ecommerce.auth_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtService.parseToken(token);

            // Verificar si el token está en la lista negra (revocado)
            String jti = claims.getId();
            Boolean revoked = redisTemplate.hasKey("revoked:" + jti);
            if (Boolean.TRUE.equals(revoked)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token revocado");
                return;
            }

            if (!jwtService.isAccessToken(claims)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Tipo de token inválido");
                return;
            }

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            @SuppressWarnings("unchecked")
            List<String> permissions = (List<String>) claims.get("permissions");

            Set<SimpleGrantedAuthority> authorities = permissions.stream()
                    .map(p -> new SimpleGrantedAuthority("PERMISSION_" + p))
                    .collect(Collectors.toSet());
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException e) {
            log.warn("JWT inválido: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido o expirado");
            return;
        }

        filterChain.doFilter(request, response);
    }
}