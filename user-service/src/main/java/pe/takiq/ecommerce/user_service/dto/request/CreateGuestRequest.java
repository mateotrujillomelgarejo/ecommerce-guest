package pe.takiq.ecommerce.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Llamado por Order Service cuando un guest inicia el checkout.
 * Implementa la misma lógica de "upsert inteligente" que tenía Customer Service.
 */
@Data
public class CreateGuestRequest {
    @NotBlank(message = "sessionId es requerido")
    private String sessionId;
    private String name;
    private String email;
    private String phone;
}
