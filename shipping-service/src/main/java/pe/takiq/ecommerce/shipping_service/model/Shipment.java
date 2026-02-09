package pe.takiq.ecommerce.shipping_service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    private String trackingNumber;
    private String status;
    private BigDecimal shippingCost;
    private String estimatedDelivery;

    private String postalCode;
    private String addressStreet;
    private String addressCity;
    private String addressCountry;

    private LocalDateTime createdAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (trackingNumber == null) {
            trackingNumber = "SHIP-" + UUID.randomUUID().toString()
                    .substring(0, 8)
                    .toUpperCase();
        }
    }
}
