package pe.takiq.ecommerce.user_service.event;

import lombok.*;
import java.time.Instant;

/**
 * Espejo del evento publicado por auth-service.
 * User-service lo escucha para crear el perfil del usuario nuevo.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserRegisteredEvent {
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private Instant registeredAt;
}
