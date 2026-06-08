package pe.takiq.ecommerce.user_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("User Service API")
                .description("Perfil, direcciones y preferencias — GUEST · CLIENT · ADMIN")
                .version("1.0.0"));
    }
}
