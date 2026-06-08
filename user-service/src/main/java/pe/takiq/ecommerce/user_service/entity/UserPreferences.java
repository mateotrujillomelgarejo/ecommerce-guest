package pe.takiq.ecommerce.user_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_preferences")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Builder.Default
    private String language = "es";

    @Builder.Default
    private String timezone = "America/Lima";

    @Builder.Default
    private String currency = "PEN";

    @Column(name = "notifications_email")
    @Builder.Default
    private Boolean notificationsEmail = true;

    @Column(name = "notifications_sms")
    @Builder.Default
    private Boolean notificationsSms = false;

    @Column(name = "dark_mode")
    @Builder.Default
    private Boolean darkMode = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
