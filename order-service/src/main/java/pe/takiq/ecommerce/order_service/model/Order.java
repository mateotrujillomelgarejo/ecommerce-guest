package pe.takiq.ecommerce.order_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
public class Order {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    private String cartId;
    private String guestId;  // ← Nuevo campo
    private String guestEmail;
    private String status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<OrderItem> items = new ArrayList<>();

    private Double totalAmount;

    private String paymentId;

    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.status == null) {
            this.status = "PENDING_PAYMENT";
        }
        this.createdAt = LocalDateTime.now();
        if (this.items == null) this.items = new ArrayList<>();
        if (this.totalAmount == null) this.totalAmount = 0.0;
    }
}
