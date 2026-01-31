package pe.takiq.ecommerce.customer_service.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
public class Guest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String sessionId;

    private String name;
    private String email;
    private String phone;

    @Embedded
    private Address address;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.expiresAt = this.createdAt.plusDays(30);
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    @Embeddable
    @Data
    public static class Address {
        private String street;
        private String city;
        private String postalCode;
        private String country;
    }
}