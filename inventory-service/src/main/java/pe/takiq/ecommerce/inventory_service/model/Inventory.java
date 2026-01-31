package pe.takiq.ecommerce.inventory_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory",
       indexes = {
           @Index(name = "idx_product_id", columnList = "productId", unique = true)
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String productId;

    @Column(nullable = false)
    private Integer availableQuantity;

    private Integer reservedQuantity;

    private Integer incomingQuantity;

    private LocalDateTime lastUpdated;

    @Column(nullable = false)
    private boolean active = true;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }
}