package pe.takiq.ecommerce.user_service.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class PreferencesResponse {
    private UUID id;
    private String language;
    private String timezone;
    private String currency;
    private Boolean notificationsEmail;
    private Boolean notificationsSms;
    private Boolean darkMode;
    private LocalDateTime createdAt;
}
