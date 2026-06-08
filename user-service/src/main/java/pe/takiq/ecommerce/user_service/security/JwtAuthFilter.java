package pe.takiq.ecommerce.user_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Verifica el JWT en cada request sin llamar a auth-service.
 * También acepta el header X-User-Id propagado por el Gateway
 * para evitar verificar el token dos veces cuando ya pasó por el Gateway.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Si el Gateway ya validó y propagó los headers, usamos eso directamente
        String userId = request.getHeader("X-User-Id");
        String userRole = request.getHeader("X-User-Role");

        if (userId != null && userRole != null) {
            var auth = new UsernamePasswordAuthenticationToken(
                userId, null,
                Set.of(new SimpleGrantedAuthority("ROLE_" + userRole))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
            return;
        }

        // Fallback: verificar JWT directamente (útil para llamadas directas sin Gateway)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                String uid = jwtUtil.getUserId(token);
                Set<String> roles = jwtUtil.getRoles(token);
                var authorities = roles.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .collect(Collectors.toList());
                var auth = new UsernamePasswordAuthenticationToken(uid, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                log.warn("Token JWT inválido en request a {}", request.getRequestURI());
            }
        }

        filterChain.doFilter(request, response);
    }
}
