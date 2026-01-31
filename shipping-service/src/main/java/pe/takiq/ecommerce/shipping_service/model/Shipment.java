package pe.takiq.ecommerce.shipping_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.UUID;


@Entity
@Table(name = "shipments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderId;

    private String trackingNumber;          // Generado automáticamente
    private String status;                  // PENDING, SHIPPED, DELIVERED, CANCELLED
    private BigDecimal shippingCost;
    private String estimatedDelivery;       // "3-5 días hábiles"
    private String postalCode;
    private String addressStreet;
    private String addressCity;
    private String addressCountry;

    private LocalDateTime createdAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING";
        if (this.trackingNumber == null) {
            this.trackingNumber = "SHIP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }
}