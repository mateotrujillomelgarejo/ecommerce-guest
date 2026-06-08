package pe.takiq.ecommerce.auth_service.event;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class UserRegisteredEvent {
    private String userId;
    private String email;
    private String role;
    private Instant registeredAt;
}
