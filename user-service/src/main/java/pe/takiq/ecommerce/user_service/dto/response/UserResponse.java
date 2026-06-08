package pe.takiq.ecommerce.user_service.dto.response;

import lombok.Data;
import pe.takiq.ecommerce.user_service.entity.UserStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class UserResponse {
    private UUID id;
    private String authUserId;
    private String sessionId;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private UserStatus status;
    private Boolean newsletterSubscribed;
    private BigDecimal totalSpent;
    private Boolean isActive;
    private LocalDateTime registrationDate;
    private LocalDateTime updatedAt;
    private List<AddressResponse> addresses;
}
