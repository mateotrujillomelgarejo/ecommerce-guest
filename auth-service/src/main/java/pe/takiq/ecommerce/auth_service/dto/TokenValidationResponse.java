package pe.takiq.ecommerce.auth_service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class TokenValidationResponse {
    private boolean valid;
    private String userId;
    private String email;
    private String role;
    private Set<String> permissions;
    private String reason;
}