package pe.takiq.ecommerce.user_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_addresses")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AddressType type;

    @Column(name = "street", nullable = false)
    private String street;

    @Column(name = "street_number")
    private String streetNumber;

    private String apartment;

    @Column(nullable = false)
    private String city;

    private String state;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(nullable = false)
    private String country;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
