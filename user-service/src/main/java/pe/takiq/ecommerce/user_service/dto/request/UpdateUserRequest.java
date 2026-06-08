package pe.takiq.ecommerce.user_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateUserRequest {
    @Size(min = 2, max = 100)
    private String firstName;
    @Size(min = 2, max = 100)
    private String lastName;
    private String phone;
    @Past
    private LocalDate dateOfBirth;
    private String gender;
    private Boolean newsletterSubscribed;
}
