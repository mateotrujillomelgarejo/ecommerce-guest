package pe.takiq.ecommerce.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        config.addAllowedOrigin("http://localhost:3000");     // React/Vite/Angular common
        config.addAllowedOrigin("http://localhost:4200");     // Angular CLI
        config.addAllowedOrigin("http://localhost:5173");     // Vite default
        config.addAllowedOrigin("http://localhost:8080");     // Si pruebas directo en gateway

        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("PATCH");
        config.addAllowedMethod("OPTIONS");

        config.addAllowedHeader("*");
        config.addExposedHeader("Authorization");
        config.addExposedHeader("Content-Type");
        config.addExposedHeader("Location");
        config.addExposedHeader("X-Requested-With");

        config.setAllowCredentials(true);

        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}