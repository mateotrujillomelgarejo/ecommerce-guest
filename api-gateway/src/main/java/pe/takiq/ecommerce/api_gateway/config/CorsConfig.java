package pe.takiq.ecommerce.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // ¡Muy importante! Pon aquí los orígenes permitidos
        config.addAllowedOrigin("http://localhost:4200");      // Angular dev
        config.addAllowedOrigin("http://localhost:8080");      // si pruebas en mismo puerto
        // config.addAllowedOrigin("https://tu-dominio.com");  // producción
        
        config.addAllowedMethod("*");           // GET, POST, PUT, DELETE, etc.
        config.addAllowedHeader("*");           // Authorization, Content-Type, etc.
        config.addExposedHeader("Authorization"); // si usas JWT o tokens
        config.setAllowCredentials(true);       // si usas cookies o auth con credenciales
        config.setMaxAge(3600L);                // cache de preflight 1 hora

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // aplica a TODAS las rutas

        return new CorsWebFilter(source);
    }
}