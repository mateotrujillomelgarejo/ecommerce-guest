package pe.takiq.ecommerce.user_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

/**
 * Usado solo internamente (DataInitializer o admin).
 * El flujo normal de registro pasa por el evento user.registered de auth-service.
 */
@Data
public class CreateUserRequest {
    @NotBlank
    @Email
    private String email;
    @NotBlank @Size(min = 2, max = 100)
    private String firstName;
    @NotBlank @Size(min = 2, max = 100)
    private String lastName;
    private String phone;
    @Past
    private LocalDate dateOfBirth;
    private String gender;
    private Boolean newsletterSubscribed = true;
}
