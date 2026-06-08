package pe.takiq.ecommerce.user_service.dto.request;

import lombok.Data;

@Data
public class UpdatePreferencesRequest {
    private String language;
    private String timezone;
    private String currency;
    private Boolean notificationsEmail;
    private Boolean notificationsSms;
    private Boolean darkMode;
}
